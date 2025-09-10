/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import config.FrontendAppConfig
import connectors.NationalDirectDebitConnector
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.{BudgetPaymentPlan, TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.audits.GetDDIs
import models.requests.{GenerateDdiRefRequest, WorkingDaysOffsetRequest}
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse}
import models.{DirectDebitSource, NddResponse, PaymentPlanType, UserAnswers}
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, YourBankDetailsPage}
import play.api.Logging
import play.api.mvc.Request
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NationalDirectDebitService @Inject()(nddConnector: NationalDirectDebitConnector,
                                           val directDebitCache: DirectDebitCacheRepository,
                                           config: FrontendAppConfig,
                                           auditService: AuditService)
                                          (implicit ec: ExecutionContext) extends Logging {
  def retrieveAllDirectDebits(id: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[NddResponse] = {
    directDebitCache.retrieveCache(id) flatMap {
      case Seq() =>
        for {
          directDebits <- nddConnector.retrieveDirectDebits()
          _ = auditService.sendEvent(GetDDIs())
          _ <- directDebitCache.cacheResponse(directDebits)(id)
        } yield directDebits
      case existingCache =>
        val response = NddResponse(existingCache.size, existingCache)
        Future.successful(response)
    }
  }

  def getEarliestPaymentDate(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers.get(YourBankDetailsPage).map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val offsetWorkingDays = calculateOffset(auddisStatus)
    val currentDate = LocalDate.now().toString

    nddConnector.getEarliestPaymentDate(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
  }

  def getEarliestPlanStartDate(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers.get(YourBankDetailsPage).map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val paymentPlanType = userAnswers.get(PaymentPlanTypePage)
      .getOrElse(throw new Exception("PaymentPlanTypePage details missing from user answers"))
    val directDebitSource = userAnswers.get(DirectDebitSourcePage)
      .getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers"))

    val offsetWorkingDays = calculateOffset(auddisStatus, paymentPlanType, directDebitSource)
    val currentDate = LocalDate.now().toString

    nddConnector.getEarliestPaymentDate(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
  }

  private[services] def calculateOffset(auddisStatus: Boolean, paymentPlanType: PaymentPlanType, directDebitSource: DirectDebitSource): Int = {
    (paymentPlanType, directDebitSource) match {
      case (VariablePaymentPlan, MGD) =>
        config.variableMgdFixedDelay
      case (BudgetPaymentPlan, SA) | (TaxCreditRepaymentPlan, TC) =>
        calculateOffset(auddisStatus)
      case _ =>
        throw new InternalServerException("User should not be on this page without being on one of the specified journeys")
    }
  }

  private[services] def calculateOffset(auddisStatus: Boolean): Int = {
    val dynamicDelay = if (auddisStatus) {
      config.paymentDelayDynamicAuddisEnabled
    } else {
      config.paymentDelayDynamicAuddisNotEnabled
    }

    config.paymentDelayFixed + dynamicDelay
  }

  def isSinglePaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.SinglePayment)

  def isBudgetPaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.BudgetPaymentPlan)

  def generateNewDdiReference(paymentReference: String)(implicit hc: HeaderCarrier): Future[GenerateDdiRefResponse] = {
    nddConnector.generateNewDdiReference(GenerateDdiRefRequest(paymentReference = paymentReference))
  }
}
