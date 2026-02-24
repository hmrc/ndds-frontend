/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.*
import models.DirectDebitSource.*
import models.requests.{ChrisSubmissionRequest, DataRequest}
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse}
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.ExistingDirectDebitIdentifierQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils.generateMacFromAnswers
import utils.{DateTimeFormats, MacGenerator}
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  appConfig: FrontendAppConfig,
  macGenerator: MacGenerator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val ua = request.userAnswers
    val confirmed = ua.get(CreateConfirmationPage).contains(true)
    val source = ua.get(DirectDebitSourcePage)
    val hasEndDate = ua.get(AddPaymentPlanEndDatePage)
    val hasFourExtraNumbers = ua.get(TellAboutThisPaymentPage)

    if (confirmed) {
      logger.warn("Attempt to access Check Your Answers confirmation; redirecting to Page Not Found")
      Redirect(routes.BackSubmissionController.onPageLoad())
    } else {
      val showPlanEndDate =
        if (hasEndDate.contains(true)) {
          PlanEndDateSummary.rowOrPlaceholder(ua)
        } else { None }
      val monthlyPaymentAmount = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        MonthlyPaymentAmountSummary.row(ua)
      } else { None }
      val finalPaymentAmount = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        FinalPaymentAmountSummary.row(ua)
      } else { None }
      val finalPaymentDate = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        FinalPaymentDateSummary.row(ua, appConfig)
      } else { None }
      val fourExtraNumbers = if (hasFourExtraNumbers.contains(false)) { None }
      else { YearEndAndMonthSummary.row(ua) }

      val list = SummaryListViewModel(
        Seq(
          DirectDebitSourceSummary.row(ua),
          PaymentPlanTypeSummary.row(ua),
          PaymentReferenceSummary.row(ua),
          TellAboutThisPaymentSummary.row(ua),
          fourExtraNumbers,
          TotalAmountDueSummary.row(ua),
          PaymentAmountSummary.row(ua),
          PaymentDateSummary.row(ua),
          PaymentsFrequencySummary.row(ua),
          RegularPaymentAmountSummary.row(ua),
          PlanStartDateSummary.row(ua),
          AddPaymentPlanEndDateSummary.row(ua),
          showPlanEndDate,
          monthlyPaymentAmount,
          finalPaymentDate,
          finalPaymentAmount
        ).flatten
      )

      val backRoute: Call = backRouteCheck(source, hasEndDate)
      Ok(view(list, DateTimeFormats.formattedCurrentDate, backRoute))
    }
  }

  private def backRouteCheck(source: Option[DirectDebitSource], hasEndDate: Option[Boolean]): Call = {
    (source, hasEndDate) match {
      case (Some(MGD) | Some(TC), _) => routes.PlanStartDateController.onPageLoad(NormalMode)
      case (Some(SA), Some(false))   => routes.AddPaymentPlanEndDateController.onPageLoad(NormalMode)
      case (Some(SA), Some(true))    => routes.PlanEndDateController.onPageLoad(NormalMode)
      case _                         => routes.PaymentDateController.onPageLoad(NormalMode)
    }
  }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val ua: UserAnswers = {
        val directDebitSource = request.userAnswers.get(DirectDebitSourcePage)
        val fourExtraNumbers = request.userAnswers.get(YearEndAndMonthPage)
        if (directDebitSource.contains(DirectDebitSource.PAYE) && fourExtraNumbers.isDefined) addFourExtraNoToPayRef(request.userAnswers)
        else request.userAnswers
      }

      validateStartAndEndDates(ua) match {
        case Some(redirect) =>
          Future.successful(redirect)
        case _ =>
          nddService
            .isDuplicatePlanSetupAmendAndAddPaymentPlan(userAnswers = ua, userId = request.userId, None, None)
            .flatMap { duplicateResponse =>
              {
                if (duplicateResponse.isDuplicate) {
                  val warningUrl = ua.get(PaymentPlanTypePage) match {
                    case Some(planType) if planType == PaymentPlanType.VariablePaymentPlan => routes.DuplicateErrorController.onPageLoad()
                    case _ => routes.DuplicateWarningForAddOrCreatePPController.onPageLoad(NormalMode)
                  }
                  Future.successful(Redirect(warningUrl))
                } else {
                  val validationResultF: Future[Either[String, Unit]] =
                    if (requiresEarliestPaymentDateCheckForSinglePlan(ua)) {
                      nddService
                        .getFutureWorkingDays(request.userAnswers, request.userId)
                        .map(earliest => validateSinglePlanDate(ua, earliest))
                    } else if (requireBudgetingPlanCheck(ua)) {
                      nddService
                        .getFutureWorkingDays(request.userAnswers, request.userId)
                        .map(earliest => validateBudgetingPlanDates(ua, earliest))
                    } else if (requireVariableAndTcPlanCheck(ua)) {
                      nddService
                        .getFutureWorkingDays(request.userAnswers, request.userId)
                        .map(earliest => validateVariableAndTcPlanDates(ua, earliest))
                    } else {
                      // No rule means proceed normally (not an error)
                      Future.successful(Right(()))
                    }

                  validationResultF.flatMap {
                    case Right(_) =>
                      processDdiReferenceGeneration(ua, request)
                    case Left(errorMessage) =>
                      logger.warn(s"Date validation failed: $errorMessage")
                      if (
                        errorMessage.contains("before earliest allowed date")
                        || errorMessage.contains("End date")
                        || errorMessage.contains("Start date")
                        || errorMessage.contains("Payment date")
                        || errorMessage.contains("missing in UserAnswers")
                      ) {
                        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
                      } else {
                        // All other errors → system page
                        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
                      }
                  }
                }
              }
            }
      }
    }

  private def validateSinglePlanDate(
    ua: UserAnswers,
    earliest: EarliestPaymentDate
  ): Either[String, Unit] = {
    val maxDateForSinglePlan = LocalDate.now().plusYears(1)
    ua.get(PaymentDatePage) match {
      case None =>
        logger.warn("No valid data available for single payment plan")
        Left("PaymentDatePage missing in UserAnswers")
      case Some(paymentDate) =>
        val earliestDate = LocalDate.parse(earliest.date)

        if (paymentDate.enteredDate.isBefore(earliestDate)) {
          logger.warn("Payment date entered is before the earliest date")
          Left(s"Payment date ${paymentDate.enteredDate} is before earliest allowed date $earliestDate")
        } else if (paymentDate.enteredDate.isAfter(maxDateForSinglePlan)) {
          Left(s"Payment date ${paymentDate.enteredDate} is after maximum allowed date $maxDateForSinglePlan")
        } else {
          Right(())
        }
    }
  }

  private def validateBudgetingPlanDates(
    ua: UserAnswers,
    earliestPlanStartDate: EarliestPaymentDate
  ): Either[String, Unit] = {

    val maybeStart = ua.get(PlanStartDatePage).map(_.enteredDate)
    val maybeEnd = ua.get(PlanEndDatePage)
    val earliest = LocalDate.parse(earliestPlanStartDate.date)
    val today = LocalDate.now()
    val maxDate = today.plusYears(1)

    maybeStart match {
      case None =>
        logger.warn("No valid data available for budget payment plan")
        Left("PlanStartDatePage missing in UserAnswers")
      case Some(start) if start.isBefore(earliest) =>
        logger.warn("Start date entered is before the earliest date")
        Left(s"Start date $start is before earliest allowed date $earliest")
      case Some(start) if start.isAfter(maxDate) =>
        logger.warn("Start date entered is after the maximum allowed date")
        Left(s"Start date $start is after maximum allowed date $maxDate")
      case Some(start) =>
        maybeEnd match {
          case Some(end) if end.isBefore(start) =>
            logger.warn("End date is before the start date")
            Left(s"End date $end is before start date $start")

          case _ =>
            Right(())
        }
    }
  }

  private def validateVariableAndTcPlanDates(
    ua: UserAnswers,
    earliest: EarliestPaymentDate
  ): Either[String, Unit] = {
    val TimeToPayMaxDays = 30

    ua.get(PlanStartDatePage).map(_.enteredDate) match {

      case None =>
        logger.warn("No valid date available for variable/TC plan")
        Left("PlanStartDatePage missing in UserAnswers")
      case Some(start) =>
        val earliestDate = LocalDate.parse(earliest.date)
        val today = LocalDate.now()
        val maxDate = today.plusDays(TimeToPayMaxDays)

        if (start.isBefore(earliestDate)) {
          logger.warn("Start date entered is before the earliest date")
          Left(s"Start date $start is before earliest allowed date for variable/TC plan $earliestDate")
        } else if (start.isAfter(maxDate)) {
          logger.warn("Start date entered is after the maximum allowed date")
          Left(s"Start date $start is after maximum allowed date $maxDate")
        } else {
          Right(())
        }
    }
  }

  private def requireBudgetingPlanCheck(ua: UserAnswers): Boolean =
    ua.get(PaymentPlanTypePage).contains(PaymentPlanType.BudgetPaymentPlan)

  private def requireVariableAndTcPlanCheck(ua: UserAnswers): Boolean =
    ua.get(PaymentPlanTypePage).exists {
      case PaymentPlanType.VariablePaymentPlan | PaymentPlanType.TaxCreditRepaymentPlan => true
      case _                                                                            => false
    }

  private def requiresEarliestPaymentDateCheckForSinglePlan(ua: UserAnswers): Boolean = {
    val optSourceType = ua.get(DirectDebitSourcePage)
    val optPaymentType = ua.get(PaymentPlanTypePage)
    optSourceType.exists {
      case OL | NIC | CT | SDLT | VAT | PAYE                                                   => true
      case DirectDebitSource.MGD if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan) => true
      case DirectDebitSource.SA if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan)  => true
      case DirectDebitSource.TC if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan)  => true
      case _                                                                                   => false
    }
  }

  private def processDdiReferenceGeneration(
    ua: UserAnswers,
    request: DataRequest[AnyContent]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    val existingDirectDebitRefEitherFuture: Future[Either[Result, String]] =
      ua.get(ExistingDirectDebitIdentifierQuery) match {
        // Existing direct debit: skip MAC and skip generateNewDdiReference
        case Some(existingDirectDebit) =>
          logger.info(s"Using existing DDI reference: ${existingDirectDebit.ddiRefNumber}")
          Future.successful(Right(existingDirectDebit.ddiRefNumber))
        // New direct debit: validate MAC and generate new DDI
        case None =>
          val maybeMac2 = generateMacFromAnswers(ua, macGenerator, appConfig.bacsNumber)
          (ua.get(pages.MacValuePage), maybeMac2) match {
            case (Some(mac1), Some(mac2)) if mac1 == mac2 =>
              nddService
                .generateNewDdiReference(required(PaymentReferencePage)(ua))
                .map(ref => Right(ref.ddiRefNumber))

            case (Some(_), Some(_)) =>
              logger.error(s"MAC validation failed for user")
              Future.successful(Left(Redirect(routes.SystemErrorController.onPageLoad())))
            case _ =>
              logger.error("MAC generation failed or MAC1 missing in UserAnswers")
              Future.successful(Left(Redirect(routes.SystemErrorController.onPageLoad())))
          }
      }

    existingDirectDebitRefEitherFuture.flatMap {
      case Right(ddiRefNumber) =>
        val chrisRequest = ChrisSubmissionRequest.buildChrisSubmissionRequest(ua, ddiRefNumber, request.userId, appConfig)
        nddService.submitChrisData(chrisRequest).flatMap { success =>
          if (success) {
            for {
              updated1 <- Future.fromTry(ua.set(CheckYourAnswerPage, GenerateDdiRefResponse(ddiRefNumber = ddiRefNumber)))
              updated2 <- Future.fromTry(updated1.set(CreateConfirmationPage, true))
              _        <- sessionRepository.set(updated2)
            } yield {
              Redirect(routes.DirectDebitConfirmationController.onPageLoad())
            }
          } else {
            Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
          }
        }

      case Left(result) =>
        Future.successful(result)
    }
  }

  private def validateStartAndEndDates(userAnswers: UserAnswers): Option[Result] = {
    val hasEndDate = userAnswers.get(AddPaymentPlanEndDatePage)
    if (hasEndDate.contains(true) && userAnswers.get(PlanEndDatePage).isEmpty) {
      Some(Redirect(routes.ConfirmAnswersErrorController.onPageLoad()))
    } else {
      None
    }
  }

  private def addFourExtraNoToPayRef(ua: UserAnswers): UserAnswers = {
    (for {
      paymentRef       <- ua.get(PaymentReferencePage)
      fourExtraNumbers <- ua.get(YearEndAndMonthPage)
      updatedPaymentRef = paymentRef + fourExtraNumbers.displayFormat
      updatedAnswers <- ua.set(PaymentReferencePage, updatedPaymentRef).toOption
    } yield updatedAnswers).getOrElse(ua)
  }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))

}
