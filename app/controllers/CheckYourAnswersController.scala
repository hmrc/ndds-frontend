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
import models.DirectDebitSource.*
import models.requests.ChrisSubmissionRequest
import models.{DirectDebitSource, PaymentPlanCalculation, PaymentPlanType, UserAnswers, YourBankDetailsWithAuddisStatus}
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, NationalDirectDebitService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{DateTimeFormats, PaymentCalculations}
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView
import utils.MacGenerator
import utils.Utils.generateMacFromAnswers
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditService: AuditService,
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
    val directDebitSource = request.userAnswers.get(DirectDebitSourcePage)
    val showStartDate = if (directDebitSource.contains(DirectDebitSource.PAYE)) {
      YearEndAndMonthSummary.row(request.userAnswers)
    } else {
      PlanStartDateSummary.row(request.userAnswers)
    }

    val list = SummaryListViewModel(
      rows = Seq(
        PaymentReferenceSummary.row(request.userAnswers),
        TotalAmountDueSummary.row(request.userAnswers),
        PaymentAmountSummary.row(request.userAnswers),
        PaymentDateSummary.row(request.userAnswers),
        PaymentsFrequencySummary.row(request.userAnswers),
        RegularPaymentAmountSummary.row(request.userAnswers),
        showStartDate,
        PlanEndDateSummary.row(request.userAnswers),
        MonthlyPaymentAmountSummary.row(request.userAnswers),
        FinalPaymentDateSummary.row(request.userAnswers, appConfig),
        FinalPaymentAmountSummary.row(request.userAnswers)
      ).flatten
    )

    val currentDate = DateTimeFormats.formattedCurrentDate
    Ok(view(list, currentDate))
  }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val ua: UserAnswers = request.userAnswers

      val maybeMac2 = generateMacFromAnswers(ua, macGenerator, appConfig.bacsNumber)

      (ua.get(pages.MacValuePage), maybeMac2) match {
        case (Some(mac1), Some(mac2)) if mac1 == mac2 =>
          logger.debug(s"MAC validation successful")
          nddService.generateNewDdiReference(required(PaymentReferencePage)).flatMap { reference =>
            val chrisRequest = buildChrisSubmissionRequest(ua, reference.ddiRefNumber)

            nddService
              .submitChrisData(chrisRequest)
              .flatMap { success =>
                if (success) {
                  logger.info(s"CHRIS submission successful for DDI Ref [${reference.ddiRefNumber}]")

                  for {
                    updatedAnswers <- Future.fromTry(ua.set(CheckYourAnswerPage, reference))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield {
                    auditService.sendSubmitDirectDebitPaymentPlan
                    logger.debug(s"Audit event sent for DDI Ref [${reference.ddiRefNumber}], service [${chrisRequest.serviceType}]")
                    Redirect(routes.DirectDebitConfirmationController.onPageLoad())
                  }
                } else {
                  logger.error(s"CHRIS submission failed for DDI Ref [${reference.ddiRefNumber}]")
                  Future.successful(
                    Redirect(routes.JourneyRecoveryController.onPageLoad())
                      .flashing("error" -> "There was a problem submitting your direct debit. Please try again later.")
                      // TODO Must not use content from outside of messages, cannot translate (also shouldn't use flashing)
                  )
                }
              }
              .recover { case ex =>
                logger.error("CHRIS submission or session update failed", ex)
                Redirect(routes.JourneyRecoveryController.onPageLoad())
                  .flashing("error" -> "There was a problem submitting your direct debit. Please try again later.")
              // TODO Must not use content from outside of messages, cannot translate (also shouldn't use flashing)
              }
          }

        case (Some(mac1), Some(mac2)) =>
          logger.error(s"MAC validation failed for user ${request.userId}")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case _ =>
          logger.error("MAC generation failed or MAC1 not found in UserAnswers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def buildChrisSubmissionRequest(
    userAnswers: UserAnswers,
    ddiReference: String
  ): ChrisSubmissionRequest = {
    implicit val ua: UserAnswers = userAnswers
    val calculationOpt: Option[PaymentPlanCalculation] =
      for {
        source <- ua.get(DirectDebitSourcePage) if source == DirectDebitSource.TC
        plan   <- ua.get(PaymentPlanTypePage) if plan == PaymentPlanType.TaxCreditRepaymentPlan
      } yield calculateTaxCreditRepaymentPlan(ua)

    ChrisSubmissionRequest(
      serviceType                     = required(DirectDebitSourcePage),
      paymentPlanType                 = ua.get(PaymentPlanTypePage).getOrElse(PaymentPlanType.SinglePaymentPlan),
      paymentPlanReferenceNumber      = None,
      paymentFrequency                = ua.get(PaymentsFrequencyPage).map(_.toString),
      yourBankDetailsWithAuddisStatus = required(YourBankDetailsPage),
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
      calculation                     = calculationOpt
    )
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
      finalPaymentDate       = Some(finalPaymentDate)
    )
  }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))

}
