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
import models.requests.*
import pages.*
import models.responses.*
import models.{DirectDebitSource, NddResponse, NextPaymentValidationResult, PaymentPlanType, UserAnswers}
import play.api.Logging
import play.api.mvc.Request
import queries.{DirectDebitReferenceQuery, PaymentPlansCountQuery}
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.Utils
import utils.Frequency

import java.time.temporal.TemporalAdjusters
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NationalDirectDebitService @Inject() (nddConnector: NationalDirectDebitConnector,
                                            val directDebitCache: DirectDebitCacheRepository,
                                            config: FrontendAppConfig,
                                            auditService: AuditService
                                           )(implicit ec: ExecutionContext)
    extends Logging {
  def retrieveAllDirectDebits(id: String)(implicit hc: HeaderCarrier, request: Request[?]): Future[NddResponse] = {
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

  def submitChrisData(submission: ChrisSubmissionRequest)(implicit hc: HeaderCarrier): Future[Boolean] = {
    nddConnector
      .submitChrisData(submission)
      .map { success =>
        success
      }
      .recover { case ex =>
        logger.error(s"Failed to submit Chris data: ${ex.getMessage}", ex)
        false
      }
  }

  def calculateFutureWorkingDays(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers
      .get(YourBankDetailsPage)
      .map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val offsetWorkingDays = calculateOffset(auddisStatus)
    val currentDate = LocalDate.now().toString

    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
  }

  def getEarliestPlanStartDate(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers
      .get(YourBankDetailsPage)
      .map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val paymentPlanType = userAnswers
      .get(PaymentPlanTypePage)
      .getOrElse(throw new Exception("PaymentPlanTypePage details missing from user answers"))
    val directDebitSource = userAnswers
      .get(DirectDebitSourcePage)
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

  // PaymentPlanTypePage used for setup journey and AmendPaymentPlanTypePage used for Amend journey
  def isSinglePaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.SinglePaymentPlan) || userAnswers
      .get(ManagePaymentPlanTypePage)
      .getOrElse("") == PaymentPlanType.SinglePaymentPlan.toString

  def isBudgetPaymentPlan(userAnswers: UserAnswers): Boolean =
    userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.BudgetPaymentPlan) || userAnswers
      .get(ManagePaymentPlanTypePage)
      .getOrElse("") == PaymentPlanType.BudgetPaymentPlan.toString

  def generateNewDdiReference(paymentReference: String)(implicit hc: HeaderCarrier): Future[GenerateDdiRefResponse] = {
    nddConnector.generateNewDdiReference(GenerateDdiRefRequest(paymentReference = paymentReference))
  }

  def retrieveDirectDebitPaymentPlans(
    directDebitReference: String
  )(implicit hc: HeaderCarrier, request: Request[?]): Future[NddDDPaymentPlansResponse] = {
    nddConnector.retrieveDirectDebitPaymentPlans(directDebitReference)
  }

  def amendPaymentPlanGuard(userAnswers: UserAnswers): Boolean =
    isSinglePaymentPlan(userAnswers) || isBudgetPaymentPlan(userAnswers)

  def isTwoDaysPriorPaymentDate(planStarDate: LocalDate)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate = LocalDate.now().toString
    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = 2)).map { futureWorkingDays =>
      {
        val twoDaysPrior = planStarDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.info(s"twoDaysPrior flag is set to: $twoDaysPrior")
        twoDaysPrior
      }
    }
  }

  def isThreeDaysPriorPlanEndDate(planEndDate: LocalDate)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate = LocalDate.now().toString
    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = 3)).map { futureWorkingDays =>
      {
        val isThreeDaysPrior = planEndDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.info(s"planEndWithinThreeDays flag is set to: $isThreeDaysPrior")
        isThreeDaysPrior
      }
    }
  }

  def calculateNextPaymentDate(
    planStartDate: LocalDate,
    planEndDateOpt: Option[LocalDate], // optional now
    paymentFrequency: Frequency
  )(implicit hc: HeaderCarrier): Future[NextPaymentValidationResult] = {

    val today = LocalDate.now()

    // Step 0: if no plan end date, skip calculation & validation
    if (planEndDateOpt.isEmpty) {
      Future.successful(
        NextPaymentValidationResult(
          potentialNextPaymentDate = None,
          nextPaymentDateValid     = true
        )
      )
    }

    for {
      // Step 1.1 – check if start date is beyond 3 working days
      isBeyondThreeDays <- isThreeDaysPriorPlanEndDate(planStartDate)

      // Step 1.2 – calculate potential next payment date
      potentialNextPaymentDate <-
        if (planStartDate.isAfter(today) && isBeyondThreeDays) {
          // Start date is after today and beyond 3 working days
          Future.successful(Some(planStartDate))
        } else {
          // Otherwise, calculate next payment date using frequency logic
          Future.successful(Some(calculateFrequencyBasedNextDate(planStartDate, today, paymentFrequency)))
        }

    } yield {
      // Step 2 – Validate potentialNextPaymentDate against planEndDate
      val nextPaymentDateValid =
        potentialNextPaymentDate.forall { next =>
          planEndDateOpt.forall(!next.isAfter(_))
        }

      NextPaymentValidationResult(
        potentialNextPaymentDate = potentialNextPaymentDate,
        nextPaymentDateValid     = nextPaymentDateValid
      )
    }
  }

  private def calculateFrequencyBasedNextDate(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  ): LocalDate = frequency match {
    case Frequency.Weekly | Frequency.Fortnightly | Frequency.FourWeekly =>
      calculateWeeklyBasedNextDate(startDate, today, frequency)
    case Frequency.Monthly | Frequency.Quarterly | Frequency.SixMonthly | Frequency.Annually =>
      calculateMonthlyBasedNextDate(startDate, today, frequency)
  }

  import java.time.temporal.ChronoUnit

  private def calculateWeeklyBasedNextDate(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  ): LocalDate = {

    val daysInWeek = 7

    val daysFrequency = frequency match {
      case Frequency.Weekly      => 7
      case Frequency.Fortnightly => 14
      case Frequency.FourWeekly  => 28
      case other =>
        throw new IllegalArgumentException(s"Invalid weekly frequency: $other")
    }

    val daysDiff = ChronoUnit.DAYS.between(startDate, today).toInt
    val paymentsTakenToDate = Math.max(0, daysDiff / daysFrequency)
    val weeksUntilNextPayment = daysFrequency / daysInWeek
    var potentialNext = startDate.plusWeeks(paymentsTakenToDate * weeksUntilNextPayment)

    if (!potentialNext.isAfter(today.plusDays(3))) {
      potentialNext = potentialNext.plusWeeks(weeksUntilNextPayment)
    }

    potentialNext
  }

  private def calculateMonthlyBasedNextDate(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  ): LocalDate = {

    val annualMonths = 12

    val monthsFrequency = frequency match {
      case Frequency.Monthly    => 1
      case Frequency.Quarterly  => 3
      case Frequency.SixMonthly => 6
      case Frequency.Annually   => 12
      case other =>
        throw new IllegalArgumentException(s"Invalid monthly frequency: $other")
    }

    val withinNext3Days = !startDate.isAfter(today.plusDays(3))
    if (startDate.isAfter(today) && withinNext3Days) {
      return startDate.plusMonths(monthsFrequency)
    }

    val yearsDiff = today.getYear - startDate.getYear

    val monthsDiff =
      if (yearsDiff == 0) today.getMonthValue - startDate.getMonthValue
      else if (yearsDiff == 1) (annualMonths - startDate.getMonthValue) + today.getMonthValue
      else (annualMonths - startDate.getMonthValue) + today.getMonthValue + ((yearsDiff - 1) * annualMonths)

    var paymentsTakenToDate = monthsDiff / monthsFrequency
    if ((monthsDiff % monthsFrequency) != 0) paymentsTakenToDate += 1

    var monthsToAdd = paymentsTakenToDate * monthsFrequency
    var potentialNext = startDate.plusMonths(monthsToAdd)

    if (!potentialNext.isAfter(today)) {
      monthsToAdd += monthsFrequency
      potentialNext = startDate.plusMonths(monthsToAdd)
    }

    if (potentialNext.getMonthValue < startDate.getMonthValue) {
      potentialNext = potentialNext.plusMonths(1).`with`(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
    }

    if (!potentialNext.isAfter(today.plusDays(3))) {
      monthsToAdd += monthsFrequency
      potentialNext = startDate.plusMonths(monthsToAdd)
      if (potentialNext.getMonthValue < startDate.getMonthValue) {
        potentialNext = potentialNext.plusMonths(1).`with`(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
      }
    }

    potentialNext
  }

  def getPaymentPlanDetails(directDebitReference: String, paymentPlanReference: String)(implicit
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[PaymentPlanResponse] = {
    nddConnector.getPaymentPlanDetails(directDebitReference, paymentPlanReference)
  }

  def isDuplicatePaymentPlan(ua: UserAnswers)(implicit hc: HeaderCarrier, request: Request[?]): Future[DuplicateCheckResponse] = {
    ua.get(PaymentPlansCountQuery) match {
      case Some(count) => {
        if (count > 1) {
          val request: PaymentPlanDuplicateCheckRequest =
            Utils.buildPaymentPlanCheckRequest(ua, ua.get(DirectDebitReferenceQuery).get)

          nddConnector.isDuplicatePaymentPlan(request.directDebitReference, request)
        } else {
          logger.info("There is only 1 payment plan so not checking duplicate as in no RDS Call")
          Future.successful(DuplicateCheckResponse(false))
        }
      }
      case None => {
        logger.error("Could not find the count of Payment Plans" + ua.get(PaymentPlansCountQuery).get)
        throw new IllegalStateException("Count of payment plans is missing or invalid")
      }
    }
  }

  def isPaymentPlanCancellable(userAnswers: UserAnswers): Boolean = {
    userAnswers
      .get(ManagePaymentPlanTypePage)
      .exists(planType =>
        Set(
          PaymentPlanType.SinglePaymentPlan.toString,
          PaymentPlanType.BudgetPaymentPlan.toString,
          PaymentPlanType.VariablePaymentPlan.toString
        ).contains(planType)
      )
  }
}
