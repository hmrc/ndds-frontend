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
import models.requests.{ChrisSubmissionRequest, GenerateDdiRefRequest, PaymentPlanDuplicateCheckRequest, WorkingDaysOffsetRequest}
import models.responses.{DirectDebitDetails, EarliestPaymentDate, GenerateDdiRefResponse, NddDDPaymentPlansResponse, PaymentPlanDetails, PaymentPlanResponse}
import models.{DirectDebitSource, NddResponse, PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.*
import play.api.Logging
import play.api.mvc.Request
import queries.{DirectDebitReferenceQuery, PaymentPlanTypeQuery}
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.Utils

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

  //PaymentPlanTypePage used for setup journey and PaymentPlanTypeQuery used for Amend journey
  def isSinglePaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.SinglePaymentPlan) || userAnswers.get(PaymentPlanTypeQuery).getOrElse("") == PaymentPlanType.SinglePaymentPlan.toString

  def isBudgetPaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.BudgetPaymentPlan) || userAnswers.get(PaymentPlanTypeQuery).getOrElse("") == PaymentPlanType.BudgetPaymentPlan.toString

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
        val isThreeDaysPrior = planEndDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.info(s"planEndWithinThreeDays flag is set to: $isThreeDaysPrior")
        isThreeDaysPrior
      }
    }
  }

  def getPaymentPlanDetails(paymentReference: String): Future[PaymentPlanResponse] = {
    //TODO *** TEMP DATA WILL BE REPLACED WITH ACTUAL DATA***
    val now = LocalDateTime.now()
    val currentDate = LocalDate.now()

    val samplePaymentPlanResponse: PaymentPlanResponse =
      PaymentPlanResponse(
        directDebitDetails = DirectDebitDetails(
          bankSortCode = "123456",
          bankAccountNumber = "12345678",
          bankAccountName = "John Doe",
          auddisFlag = true
        ),
        paymentPlanDetails = PaymentPlanDetails(
          hodService = "CESA",
          planType = PaymentPlanType.BudgetPaymentPlan.toString,
          paymentReference = paymentReference,
          submissionDateTime = now.minusDays(5), //Some(now.minusDays(5)),
          scheduledPaymentAmount = 120.00,
          scheduledPaymentStartDate = currentDate.plusDays(3), //Some(LocalDate.now().minusMonths(8)),
          initialPaymentStartDate = now.plusDays(5), //Some(LocalDateTime.now().plusDays(1)),
          initialPaymentAmount = Some(BigDecimal(50.00)),
          scheduledPaymentEndDate = currentDate.plusDays(4), //Some(LocalDate.now().plusMonths(12)),
          scheduledPaymentFrequency = Some(PaymentsFrequency.Weekly.toString),
          suspensionStartDate = None,
          suspensionEndDate = None,
          balancingPaymentAmount = Some(BigDecimal(25.00)),
          balancingPaymentDate = Some(LocalDateTime.now().plusMonths(13)),
          totalLiability = Some(780.00), //Some(BigDecimal(1825.50)),
          paymentPlanEditable = true
        )
      )
    Future.successful(samplePaymentPlanResponse)
  }

  def amendmentMade(ua: UserAnswers): Boolean = {
    ua.get(PaymentPlanTypeQuery) match {
      case Some("singlePaymentPlan") => amountChanged(ua) || startDateChanged(ua)
      case Some("budgetPaymentPlan") => amountChanged(ua) || endDateChanged(ua)
      case _ =>
        logger.warn(s"Unexpected / missing plan type in amend journey; applying permissive check.")
        amountChanged(ua) || startDateChanged(ua) || endDateChanged(ua)
    }
  }

  private def amountChanged(ua: UserAnswers): Boolean = {
    (ua.get(AmendPaymentAmountPage), ua.get(NewAmendPaymentAmountPage)) match {
      case (Some(amend), Some(newAmend)) => amend.compare(newAmend) != 0
      case (None, Some(_)) => true
      case _ => false
    }
  }

  private def startDateChanged(ua: UserAnswers): Boolean = {
    (ua.get(AmendPlanStartDatePage), ua.get(NewAmendPlanStartDatePage)) match {
      case (Some(amend), Some(newAmend)) => amend != newAmend
      case (None, Some(_)) => true
      case _ => false
    }
  }

  private def endDateChanged(ua: UserAnswers): Boolean = {
    (ua.get(AmendPlanEndDatePage), ua.get(NewAmendPlanEndDatePage)) match {
      case (Some(amend), Some(newAmend)) => amend != newAmend
      case (None, Some(_)) => true
      case _ => false
    }
  }

  def isDuplicatePaymentPlan(ua: UserAnswers)
                            (implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] = {
    if (
      (amountChanged(ua) && (isSinglePaymentPlan(ua) || isBudgetPaymentPlan(ua))) ||
        (isSinglePaymentPlan(ua) && startDateChanged(ua))
    ) {
      val countPaymentPlans = ua.get(DirectDebitSummaryPage).get

      if (countPaymentPlans > 1) {
        val request: PaymentPlanDuplicateCheckRequest =
          Utils.buildPaymentPlanCheckRequest(ua, ua.get(DirectDebitReferenceQuery).get)

        nddConnector.isDuplicatePaymentPlan(request.directDebitReference, request)
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
  }
}
