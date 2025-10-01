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
import models.requests.{ChrisSubmissionRequest, GenerateDdiRefRequest, WorkingDaysOffsetRequest}
import models.responses.{DirectDebitDetails, EarliestPaymentDate, GenerateDdiRefResponse, NddDDPaymentPlansResponse, PaymentPlanDetails, PaymentPlanResponse}
import models.{DirectDebitSource, NddResponse, PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.{AmendPaymentPlanTypePage, DirectDebitSourcePage, PaymentPlanTypePage, YourBankDetailsPage}
import play.api.Logging
import play.api.mvc.Request
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.{LocalDate, LocalDateTime}
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

  def submitChrisData(submission: ChrisSubmissionRequest)
                     (implicit hc: HeaderCarrier): Future[Boolean] = {
    nddConnector.submitChrisData(submission).map { success =>
      success
    }.recover { case ex =>
      logger.error(s"Failed to submit Chris data: ${ex.getMessage}", ex)
      false
    }
  }

  def calculateFutureWorkingDays(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers.get(YourBankDetailsPage).map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val offsetWorkingDays = calculateOffset(auddisStatus)
    val currentDate = LocalDate.now().toString

    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
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

    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
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
    logger.info(s"Calculate offset Auddis flag: $auddisStatus")
    val dynamicDelay = if (auddisStatus) {
      config.paymentDelayDynamicAuddisEnabled
    } else {
      config.paymentDelayDynamicAuddisNotEnabled
    }

    config.paymentDelayFixed + dynamicDelay
  }

  //PaymentPlanTypePage used for setup journey and AmendPaymentPlanTypePage used for Amend journey
  def isSinglePaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.SinglePaymentPlan) || userAnswers.get(AmendPaymentPlanTypePage).getOrElse("") == PaymentPlanType.SinglePaymentPlan.toString

  def isBudgetPaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.BudgetPaymentPlan) || userAnswers.get(AmendPaymentPlanTypePage).getOrElse("") == PaymentPlanType.BudgetPaymentPlan.toString

  def generateNewDdiReference(paymentReference: String)(implicit hc: HeaderCarrier): Future[GenerateDdiRefResponse] = {
    nddConnector.generateNewDdiReference(GenerateDdiRefRequest(paymentReference = paymentReference))
  }

  def retrieveDirectDebitPaymentPlans(directDebitReference: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[NddDDPaymentPlansResponse] = {
    nddConnector.retrieveDirectDebitPaymentPlans(directDebitReference)
  }

  def amendPaymentPlanGuard(userAnswers: UserAnswers): Boolean =
    isSinglePaymentPlan(userAnswers) || isBudgetPaymentPlan(userAnswers)

  def isTwoDaysPriorPaymentDate(planStarDate: LocalDate)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate = LocalDate.now().toString
    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = 2)).map {
      futureWorkingDays => {
        val twoDaysPrior = planStarDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.info(s"twoDaysPrior flag is set to: $twoDaysPrior")
        twoDaysPrior
      }
    }
  }

  def isThreeDaysPriorPlanEndDate(planEndDate: LocalDate)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate = LocalDate.now().toString
    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = 3)).map {
      futureWorkingDays => {
        val isThreeDaysPrior =  planEndDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.info(s"planEndWithinThreeDays flag is set to: $isThreeDaysPrior")
        isThreeDaysPrior
      }
    }
  }

  def getPaymentPlanDetails(paymentPlanReference: String): Future[PaymentPlanResponse] = {
    //TODO *** TEMP DATA WILL BE REPLACED WITH ACTUAL DATA***
    val now = LocalDateTime.now()
    val currentDate = LocalDate.now()

    val samplePaymentPlanResponse: PaymentPlanResponse =
      PaymentPlanResponse(
        directDebitDetails = DirectDebitDetails(
          bankSortCode = Some("123456"),
          bankAccountNumber = Some("12345678"),
          bankAccountName = Some("John Doe"),
          auDdisFlag = true,
          submissionDateTime = LocalDateTime.now()
        ),
        paymentPlanDetails = PaymentPlanDetails(
          hodService = "CESA",
          planType = PaymentPlanType.BudgetPaymentPlan.toString,
          paymentReference = "paymentReference", //Payment reference
          submissionDateTime = now.minusDays(5), //Date set up
          scheduledPaymentAmount = 120.00, //Payment amount or Regular payment amount or Monthly payment amount
          scheduledPaymentStartDate = currentDate.plusDays(4), //Payment date or Plan start date
          scheduledPaymentEndDate = currentDate.plusDays(5), //Plan end date
          scheduledPaymentFrequency = Some("Weekly"), //Frequency of payments
          suspensionStartDate = Some(currentDate.plusDays(2)), //Suspend start date
          suspensionEndDate = None, //Suspend end date
          balancingPaymentAmount = Some(BigDecimal(25.00)), //Final payment amount
          totalLiability = None, //Total amount due
          balancingPaymentDate = Some(currentDate.plusMonths(13)),
          initialPaymentStartDate = Some(currentDate.plusDays(5)),
          initialPaymentAmount = Some(BigDecimal(50.00)),
          paymentPlanEditable = true
        )
      )
    Future.successful(samplePaymentPlanResponse)
  }

}
