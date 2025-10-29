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
import models.{DirectDebitSource, NddResponse, NextPaymentValidationResult, NextPaymentValidationResult2, PaymentPlanType, UserAnswers}
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
import java.time.temporal.ChronoUnit

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
        logger.warn(s"Failed to submit Chris data", ex)
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
    logger.debug(s"Calculate offset Auddis flag: $auddisStatus")
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
        logger.debug(s"twoDaysPrior flag is set to: $twoDaysPrior")
        twoDaysPrior
      }
    }
  }

  def isThreeDaysPriorPlanEndDate(planEndDate: LocalDate)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val currentDate = LocalDate.now().toString
    nddConnector.getFutureWorkingDays(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = 3)).map { futureWorkingDays =>
      {
        val isThreeDaysPrior = planEndDate.isAfter(LocalDate.parse(futureWorkingDays.date))
        logger.debug(s"planEndWithinThreeDays flag is set to: $isThreeDaysPrior")
        isThreeDaysPrior
      }
    }
  }

  def calculateNextPaymentDate(
    planStartDate: LocalDate,
    planEndDate: LocalDate,
    paymentFrequency: Frequency
  )(implicit hc: HeaderCarrier): Future[NextPaymentValidationResult] = {

    val today = LocalDate.now()

    for {
      // Step 1.1 – check if start date is beyond 3 working days
      isBeyondThreeDays <- isThreeDaysPriorPlanEndDate(planStartDate)

      // Step 1.2 – calculate potential next payment date
      potentialNextPaymentDate <-
        if (planStartDate.isAfter(today) && isBeyondThreeDays) {
          // Start date is after today and beyond 3 working days
          // Skip frequency logic — per business rule Step 1.1
          Future.successful(planStartDate)
        } else {
          //  Otherwise, calculate next payment date using frequency logic (Step 1.2)
          Future.successful(
            calculateFrequencyBasedNextDate(planStartDate, today, paymentFrequency)
          )
        }

    } yield {
      // Step 2 – Validate potentialNextPaymentDate against planEndDate
      val nextPaymentDateValid =
        if (planEndDate != null) {
          !potentialNextPaymentDate.isAfter(planEndDate)
        } else {
          true
        }

      NextPaymentValidationResult(
        potentialNextPaymentDate = potentialNextPaymentDate,
        nextPaymentDateValid     = nextPaymentDateValid
      )
    }
  }

  def calculateNextPaymentDate2(
    planStartDate: LocalDate,
    planEndDate: Option[LocalDate],
    paymentFrequency: Frequency
  )(implicit hc: HeaderCarrier): Future[NextPaymentValidationResult2] = {
    planEndDate.fold {
      // when planEndDate is open-ended date or None
      Future.successful(
        NextPaymentValidationResult2(
          potentialNextPaymentDate = None,
          nextPaymentDateValid     = true
        )
      )
    } { endDate => // when planEndDate has a future date
      {
        val today = LocalDate.now()

        for {
          // Step 1.1 – check if start date is beyond 3 working days
          isBeyondThreeDays <- isThreeDaysPriorPlanEndDate(planStartDate)

          // Step 1.2 – calculate potential next payment date
          potentialNextPaymentDate <-
            if (planStartDate.isAfter(today) && isBeyondThreeDays) {
              // Start date is after today and beyond 3 working days
              // Skip frequency logic — per business rule Step 1.1
              Future.successful(planStartDate)
            } else {
              //  Otherwise, calculate next payment date using frequency logic (Step 1.2)
              paymentFrequency match {
                case Frequency.Weekly | Frequency.Fortnightly | Frequency.FourWeekly =>
                  calculateWeeklyBasedNextDate2(planStartDate, today, paymentFrequency)

                case Frequency.Monthly | Frequency.Quarterly | Frequency.SixMonthly | Frequency.Annually =>
                  calculateMonthlyBasedNextDate2(planStartDate, today, paymentFrequency)
              }
            }

        } yield {
          // Step 2 – Validate potentialNextPaymentDate against planEndDate
          val nextPaymentDateValid = !potentialNextPaymentDate.isAfter(endDate)

          NextPaymentValidationResult2(
            potentialNextPaymentDate = Some(potentialNextPaymentDate),
            nextPaymentDateValid     = nextPaymentDateValid
          )
        }
      }
    }
  }

  private def calculateWeeklyBasedNextDate2(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  )(implicit hc: HeaderCarrier): Future[LocalDate] = {

    val daysInWeek = 7

    // map enum to days per payment cycle
    val daysFrequency = frequency match {
      case Frequency.Weekly      => 7
      case Frequency.Fortnightly => 14
      case Frequency.FourWeekly  => 28
      case other =>
        throw new IllegalArgumentException(s"Invalid weekly frequency: $other")
    }

    // Step 1 – find days difference
    val daysDiff = Math.abs(ChronoUnit.DAYS.between(today, startDate)).toInt

    // Step 2 – number of payments taken to date
    val paymentsTakenToDate = Math.max(0, Math.ceil(daysDiff.toDouble / daysFrequency))

    // Step 3 – number of weeks until next payment
    val weeksUntilNextPayment = daysFrequency / daysInWeek

    // Step 4 – potential next payment date
    val potentialNext = startDate.plusWeeks(paymentsTakenToDate.toLong * weeksUntilNextPayment)

    // Step 5 – if within 3 working days → add one cycle
    isThreeDaysPriorPlanEndDate(potentialNext).map { isBeyondThreeWorkingDays =>
      if (isBeyondThreeWorkingDays) {
        logger.debug(
          s"""|[calculateWeeklyBasedNextDate]
              |  Frequency: $frequency
              |  Start date: $startDate
              |  Today: $today
              |  Payments taken so far: $paymentsTakenToDate
              |  Next payment date: $potentialNext
              |""".stripMargin
        )
        potentialNext
      } else {
        val newPotentialNextDate = potentialNext.plusWeeks(weeksUntilNextPayment)
        logger.debug(
          s"""|[calculateWeeklyBasedNextDate]
              |  Frequency: $frequency
              |  Start date: $startDate
              |  Today: $today
              |  Payments taken so far: $paymentsTakenToDate
              |  Next payment date: $newPotentialNextDate
              |""".stripMargin
        )
        newPotentialNextDate
      }
    }
  }

  private def calculateMonthlyBasedNextDate2(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  )(implicit hc: HeaderCarrier): Future[LocalDate] = {

    val annualMonths = 12

    val monthsFrequency = frequency match {
      case Frequency.Monthly    => 1
      case Frequency.Quarterly  => 3
      case Frequency.SixMonthly => 6
      case Frequency.Annually   => 12
      case other =>
        throw new IllegalArgumentException(s"Invalid monthly frequency: $other")
    }

    // Step 1: Plan start date is after today and due to start within next 3 working days
    val initialPotentialStartDate = startDate.plusMonths(monthsFrequency)

    // Step 2:
    // If the plan start date is equal to or before today, determine how many months have passed
    // since the plan began to calculate how many payments have been taken to date.
    // Use the current and start years to compute the difference correctly even if the plan
    // spans multiple calendar years.
    val (potentialNextFromHistoryOpt, numberOfMonthsToAdd) =
      if (!startDate.isAfter(today)) {

        val startYear = startDate.getYear
        val currentYear = today.getYear
        val startMonth = startDate.getMonthValue
        val currentMonth = today.getMonthValue
        val yearsDiff = currentYear - startYear

        val monthsDiff =
          if (yearsDiff == 0) {
            currentMonth - startMonth
          } else if (yearsDiff == 1) {
            (annualMonths - startMonth) + currentMonth
          } else {
            ((annualMonths - startMonth) + currentMonth) + ((yearsDiff - 1) * annualMonths)
          }

        val basePaymentsTaken = monthsDiff / monthsFrequency
        val isPaymentDueThisMonth = monthsDiff % monthsFrequency == 0
        val paymentsTaken =
          if (isPaymentDueThisMonth) {
            basePaymentsTaken
          } else {
            basePaymentsTaken + 1
          }

        val monthsToAddBase = paymentsTaken * monthsFrequency
        val potentialNextBase = startDate.plusMonths(monthsToAddBase)
        val potentialNext =
          if (potentialNextBase.isAfter(today)) {
            potentialNextBase
          } else {
            potentialNextBase.plusMonths(monthsFrequency)
          }

        (Some(potentialNext), monthsToAddBase)
      } else (None, 0)

    // Step 3:
    // Adjust for shorter months where the calculated next date falls on a day
    // earlier than the plan start date's day of month (e.g. 31 Jan → 28 Feb).
    // In such cases, move the potential next payment date to the 1st day of the
    // following month to maintain consistency with monthly cycles.

    val baseNextDate = potentialNextFromHistoryOpt.getOrElse(initialPotentialStartDate)

    val adjustedNextDate =
      if (initialPotentialStartDate.getDayOfMonth < startDate.getDayOfMonth) {
        val nextMonth = baseNextDate.plusMonths(1)
        nextMonth.withDayOfMonth(1)
      } else {
        initialPotentialStartDate
      }

    // Step 4 – If the Potential Next Payment Date within 3 working days,
    // skip this cycle and calculate a new potential next date by adding an extra
    // frequency period (Start Date + Months(Number of Months To Add + Months Frequency)).
    // Reapply the short-month adjustment logic (same as Step 3) if the resulting date’s
    // day of month is less than the plan start date’s day — in that case, move the
    // date to the 1st of the following month.

    isThreeDaysPriorPlanEndDate(adjustedNextDate).map { isWithinNextThreeWorkingDays =>
      if (isWithinNextThreeWorkingDays) {
        val potentialNext = startDate.plusMonths(numberOfMonthsToAdd + monthsFrequency)

        if (potentialNext.getDayOfMonth < startDate.getDayOfMonth) {
          val nextMonth = potentialNext.plusMonths(1)
          val nextMonthFirstDay = nextMonth.withDayOfMonth(1)
          logger.debug(
            s"""|[calculateMonthlyBasedNextDate]
                |  Frequency: $frequency
                |  Start date: $startDate
                |  Today: $today
                |  Next payment date: $nextMonthFirstDay
                |""".stripMargin
          )
          nextMonthFirstDay
        } else {
          logger.debug(
            s"""|[calculateMonthlyBasedNextDate]
                |  Frequency: $frequency
                |  Start date: $startDate
                |  Today: $today
                |  Next payment date: $potentialNext
                |""".stripMargin
          )
          potentialNext
        }
      } else {
        logger.debug(
          s"""|[calculateMonthlyBasedNextDate]
              |  Frequency: $frequency
              |  Start date: $startDate
              |  Today: $today
              |  Next payment date: $adjustedNextDate
              |""".stripMargin
        )
        adjustedNextDate
      }
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

  private def calculateWeeklyBasedNextDate(
    startDate: LocalDate,
    today: LocalDate,
    frequency: Frequency
  ): LocalDate = {

    val daysInWeek = 7

    // map enum to days per payment cycle
    val daysFrequency = frequency match {
      case Frequency.Weekly      => 7
      case Frequency.Fortnightly => 14
      case Frequency.FourWeekly  => 28
      case other =>
        throw new IllegalArgumentException(s"Invalid weekly frequency: $other")
    }

    // Step 1 – find days difference
    val daysDiff = ChronoUnit.DAYS.between(startDate, today).toInt

    // Step 2 – number of payments taken to date
    val paymentsTakenToDate = Math.max(0, daysDiff / daysFrequency)

    // Step 3 – number of weeks until next payment
    val weeksUntilNextPayment = daysFrequency / daysInWeek

    // Step 4 – potential next payment date
    var potentialNext = startDate.plusWeeks(paymentsTakenToDate.toLong * weeksUntilNextPayment)

    // Step 5 – if within 3 working days → add one cycle
    if (!potentialNext.isAfter(today.plusDays(3))) {
      potentialNext = potentialNext.plusWeeks(weeksUntilNextPayment)
    }

    logger.debug(
      s"""|[calculateWeeklyBasedNextDate]
          |  Frequency: $frequency
          |  Start date: $startDate
          |  Today: $today
          |  Payments taken so far: $paymentsTakenToDate
          |  Next payment date: $potentialNext
          |""".stripMargin
    )

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

    // Step 1: If the plan start date is after today and due to start within next 3 working days
    val withinNext3Days = !startDate.isAfter(today.plusDays(3))
    if (startDate.isAfter(today) && withinNext3Days) {
      return startDate.plusMonths(monthsFrequency)
    }

    // Step 2: Calculate months difference based on year difference
    val yearsDiff = today.getYear - startDate.getYear

    val monthsDiff =
      if (yearsDiff == 0) {
        today.getMonthValue - startDate.getMonthValue
      } else if (yearsDiff == 1) {
        (annualMonths - startDate.getMonthValue) + today.getMonthValue
      } else {
        (annualMonths - startDate.getMonthValue) + today.getMonthValue + ((yearsDiff - 1) * annualMonths)
      }

    // Step 3: Calculate how many payments have been taken to-date
    var paymentsTakenToDate = monthsDiff / monthsFrequency

    // If payment is not due this month (i.e. remainder months exist), then add 1
    if ((monthsDiff % monthsFrequency) != 0) {
      paymentsTakenToDate += 1
    }

    // Step 4: Calculate number of months to add and potential next date
    var monthsToAdd = paymentsTakenToDate * monthsFrequency
    var potentialNext = startDate.plusMonths(monthsToAdd)

    // Step 5: If potential next payment date <= today → already taken this month
    if (!potentialNext.isAfter(today)) {
      monthsToAdd += monthsFrequency
      potentialNext = startDate.plusMonths(monthsToAdd)
    }

    // Step 6: If potential next payment month is before the start month, move to 1st of next month
    if (potentialNext.getMonthValue < startDate.getMonthValue) {
      potentialNext = potentialNext
        .plusMonths(1)
        .`with`(TemporalAdjusters.firstDayOfMonth())
    }

    // Step 7: If potential next payment date is within next 3 working days, move one more frequency ahead
    if (!potentialNext.isAfter(today.plusDays(3))) {
      monthsToAdd += monthsFrequency
      potentialNext = startDate.plusMonths(monthsToAdd)

      if (potentialNext.getMonthValue < startDate.getMonthValue) {
        potentialNext = potentialNext
          .plusMonths(1)
          .`with`(TemporalAdjusters.firstDayOfMonth())
      }
    }

    logger.debug(
      s"""|[calculateMonthlyBasedNextDate]
          |  Frequency: $frequency
          |  Start date: $startDate
          |  Today: $today
          |  Months diff: $monthsDiff
          |  Payments to date: $paymentsTakenToDate
          |  Next payment date: $potentialNext
          |""".stripMargin
    )

    potentialNext
  }

  def getPaymentPlanDetails(directDebitReference: String, paymentPlanReference: String)(implicit
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[PaymentPlanResponse] = {
    nddConnector.getPaymentPlanDetails(directDebitReference, paymentPlanReference)
  }

  def lockPaymentPlan(directDebitReference: String, paymentPlanReference: String)(implicit hc: HeaderCarrier): Future[AmendLockResponse] = {
    nddConnector.lockPaymentPlan(directDebitReference, paymentPlanReference)
  }

  def isDuplicatePaymentPlan(ua: UserAnswers)(implicit hc: HeaderCarrier, request: Request[?]): Future[DuplicateCheckResponse] = {
    ua.get(PaymentPlansCountQuery) match {
      case Some(count) =>
        if (count > 1) {
          val request: PaymentPlanDuplicateCheckRequest =
            Utils.buildPaymentPlanCheckRequest(ua, ua.get(DirectDebitReferenceQuery).get)

          nddConnector.isDuplicatePaymentPlan(request.directDebitReference, request)
        } else {
          logger.debug("There is only 1 payment plan so not checking duplicate as in no RDS Call")
          Future.successful(DuplicateCheckResponse(false))
        }
      case None =>
        logger.error(s"Could not find the count of Payment Plans: ${ua.get(PaymentPlansCountQuery)}")
        throw new IllegalStateException("Count of payment plans is missing or invalid")
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

  def suspendPaymentPlanGuard(userAnswers: UserAnswers): Boolean =
    isBudgetPaymentPlan(userAnswers)

}
