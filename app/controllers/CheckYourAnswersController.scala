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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.*
import models.DirectDebitSource.*
import models.audits.{AddPaymentPlanAudit, NewDirectDebitAudit}
import models.requests.ChrisSubmissionRequest
import models.responses.GenerateDdiRefResponse
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.ExistingDirectDebitIdentifierQuery
import repositories.{DirectDebitCacheRepository, SessionRepository}
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils.generateMacFromAnswers
import utils.{DateTimeFormats, MacGenerator, PaymentCalculations}
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  val directDebitCache: DirectDebitCacheRepository,
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

    if (confirmed) {
      logger.warn("Attempt to access Check Your Answers confirmation; redirecting.")
      Redirect(routes.BackSubmissionController.onPageLoad())
    } else {
      val showStartDate = if (source.contains(DirectDebitSource.PAYE)) { YearEndAndMonthSummary.row(ua) }
      else { PlanStartDateSummary.row(ua) }
      val showPlanEndDate = if (hasEndDate.contains(false)) { None }
      else { PlanEndDateSummary.row(ua) }
      val monthlyPaymentAmount = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        MonthlyPaymentAmountSummary.row(ua)
      } else { None }
      val finalPaymentAmount = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        FinalPaymentAmountSummary.row(ua)
      } else { None }
      val finalPaymentDate = if (ua.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
        FinalPaymentDateSummary.row(ua, appConfig)
      } else { None }

      val list = SummaryListViewModel(
        Seq(
          DirectDebitSourceSummary.row(ua),
          PaymentPlanTypeSummary.row(ua),
          PaymentReferenceSummary.row(ua),
          TotalAmountDueSummary.row(ua),
          PaymentAmountSummary.row(ua),
          PaymentDateSummary.row(ua),
          PaymentsFrequencySummary.row(ua),
          RegularPaymentAmountSummary.row(ua),
          showStartDate,
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
      implicit val ua: UserAnswers = request.userAnswers

      val ddiRefEitherFuture: Future[Either[Result, String]] = ua.get(ExistingDirectDebitIdentifierQuery) match {
        // Existing direct debit: skip MAC and skip generateNewDdiReference
        case Some(paymentPlanIdentifier) =>
          logger.debug(s"Using existing DDI reference: $paymentPlanIdentifier")
          Future.successful(Right(paymentPlanIdentifier))

        // New direct debit: validate MAC and generate new DDI
        case None =>
          val maybeMac2 = generateMacFromAnswers(ua, macGenerator, appConfig.bacsNumber)

          (ua.get(pages.MacValuePage), maybeMac2) match {
            case (Some(mac1), Some(mac2)) if mac1 == mac2 =>
              logger.debug("MAC validation successful")
              nddService
                .generateNewDdiReference(required(PaymentReferencePage))
                .map(ref => Right(ref.ddiRefNumber))

            case (Some(_), Some(_)) =>
              logger.error(s"MAC validation failed for user ${request.userId}")
              Future.successful(
                Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              )

            case _ =>
              logger.error("MAC generation failed or MAC1 missing in UserAnswers")
              Future.successful(
                Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              )
          }
      }

      ddiRefEitherFuture.flatMap {
        case Left(redirect) =>
          Future.successful(redirect)

        case Right(ddiReference) =>
          buildChrisSubmissionRequest(ua, ddiReference, request.userId).flatMap { chrisRequest =>
            nddService.submitChrisData(chrisRequest).flatMap { success =>
              if (success) {
                for {
                  updated1 <- Future.fromTry(ua.set(CheckYourAnswerPage, GenerateDdiRefResponse(ddiRefNumber = ddiReference)))
                  updated2 <- Future.fromTry(updated1.set(CreateConfirmationPage, true))
                  _        <- sessionRepository.set(updated2)
                } yield {
                  Redirect(routes.DirectDebitConfirmationController.onPageLoad())
                }
              } else {
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              }
            }
          }
      }
    }

  private def buildChrisSubmissionRequest(
    userAnswers: UserAnswers,
    ddiReference: String,
    userId: String
  ): Future[ChrisSubmissionRequest] = {
    implicit val ua: UserAnswers = userAnswers
    val calculationOpt: Option[PaymentPlanCalculation] =
      for {
        source <- ua.get(DirectDebitSourcePage) if source == DirectDebitSource.TC
        plan   <- ua.get(PaymentPlanTypePage) if plan == PaymentPlanType.TaxCreditRepaymentPlan
      } yield calculateTaxCreditRepaymentPlan(ua)

    val existingDDIOpt: Option[String] = ua.get(ExistingDirectDebitIdentifierQuery)
    val hasExistingDDI: Boolean = existingDDIOpt.isDefined
    val auditType = if (hasExistingDDI) {
      Some(AddPaymentPlanAudit)
    } else {
      Some(NewDirectDebitAudit)
    }
    val bankDetailsWithAudisFuture = existingDDIOpt match {
      case Some(directDebitReferenceIdentifier) =>
        directDebitCache
          .getDirectDebit(directDebitReferenceIdentifier)(userId)
          .map(debit =>
            YourBankDetailsWithAuddisStatus(
              accountHolderName = debit.bankAccountName,
              sortCode          = debit.bankSortCode,
              accountNumber     = debit.bankAccountNumber,
              auddisStatus      = debit.auDdisFlag,
              accountVerified   = true
            )
          )
      case _ => Future.successful(required(YourBankDetailsPage))
    }

    bankDetailsWithAudisFuture.map { bankDetails =>
      ChrisSubmissionRequest(
        serviceType                     = required(DirectDebitSourcePage),
        paymentPlanType                 = ua.get(PaymentPlanTypePage).getOrElse(PaymentPlanType.SinglePaymentPlan),
        paymentPlanReferenceNumber      = None,
        paymentFrequency                = ua.get(PaymentsFrequencyPage).map(_.toString),
        yourBankDetailsWithAuddisStatus = bankDetails,
        planStartDate                   = ua.get(PlanStartDatePage),
        planEndDate                     = ua.get(PlanEndDatePage),
        paymentDate                     = ua.get(PaymentDatePage),
        yearEndAndMonth                 = ua.get(YearEndAndMonthPage),
        ddiReferenceNo                  = ddiReference,
        paymentReference                = required(PaymentReferencePage),
        totalAmountDue                  = ua.get(TotalAmountDuePage),
        paymentAmount                   = ua.get(PaymentAmountPage),
        regularPaymentAmount            = ua.get(RegularPaymentAmountPage),
        amendPaymentAmount              = None,
        calculation                     = calculationOpt,
        suspensionPeriodRangeDate       = None,
        addPlan                         = hasExistingDDI,
        auditType                       = auditType,
        bankAccountType                 = ua.get(PersonalOrBusinessAccountPage)
      )
    }
  }

  private def calculateTaxCreditRepaymentPlan(userAnswers: UserAnswers): PaymentPlanCalculation = {
    implicit val ua: UserAnswers = userAnswers

    val totalAmountDue = required(TotalAmountDuePage)
    val planStartDate = required(PlanStartDatePage)

    val regularPaymentAmount = PaymentCalculations.calculateRegularPaymentAmount(
      totalAmountDueInput   = totalAmountDue,
      totalNumberOfPayments = appConfig.tcTotalNumberOfPayments
    )

    val finalPaymentAmount = PaymentCalculations.calculateFinalPayment(
      totalAmountDue        = totalAmountDue,
      regularPaymentAmount  = BigDecimal(regularPaymentAmount),
      numberOfEqualPayments = appConfig.tcNumberOfEqualPayments
    )

    val secondPaymentDate = PaymentCalculations.calculateSecondPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset  = appConfig.tcMonthsUntilSecondPayment
    )

    val penultimatePaymentDate = PaymentCalculations.calculatePenultimatePaymentDate(
      planStartDate                = planStartDate.enteredDate,
      penultimateInstallmentOffset = appConfig.tcMonthsUntilPenultimatePayment
    )

    val finalPaymentDate = PaymentCalculations.calculateFinalPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset  = appConfig.tcMonthsUntilFinalPayment
    )

    logger.debug(s"Regular Payment: £$regularPaymentAmount, Final Payment: £$finalPaymentAmount")
    logger.debug(s"Second: $secondPaymentDate, Penultimate: $penultimatePaymentDate, Final: $finalPaymentDate")

    PaymentPlanCalculation(
      regularPaymentAmount   = Some(BigDecimal(regularPaymentAmount)),
      finalPaymentAmount     = Some(finalPaymentAmount),
      secondPaymentDate      = Some(secondPaymentDate),
      penultimatePaymentDate = Some(penultimatePaymentDate),
      finalPaymentDate       = Some(finalPaymentDate),
      monthlyPaymentAmount   = MonthlyPaymentAmountSummary.getMonthlyPaymentAmount(userAnswers)
    )
  }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))

}
