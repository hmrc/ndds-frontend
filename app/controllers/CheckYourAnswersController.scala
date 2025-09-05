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

import scala.concurrent.{ExecutionContext, Future}


class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            auditService: AuditService,
                                            nddService: NationalDirectDebitService,
                                            sessionRepository: SessionRepository,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            appConfig: FrontendAppConfig
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
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
          MonthlyPaymentAmountDueSummary.row(request.userAnswers),
          FinalPaymentDateSummary.row(request.userAnswers, appConfig),
          FinalPaymentAmountDueSummary.row(request.userAnswers)
        ).flatten
      )

      val currentDate = DateTimeFormats.formattedCurrentDate
      Ok(view(list, currentDate))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      // 1. Generate a new DDI reference
      reference <- nddService.generateNewDdiReference(
        request.userAnswers.get(PaymentReferencePage)
          .getOrElse(throw new Exception("Missing details: PaymentReferencePage"))
      )

      // 2. Build ChrisSubmissionRequest with calculations
      submission = buildChrisSubmissionRequest(request.userAnswers, reference.ddiRefNumber)

      // 3. Send to backend
      _ <- nddService.submitChrisData(submission)

      // 4. Update session with reference
      updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckYourAnswerPage, reference))
      _ <- sessionRepository.set(updatedAnswers)

      // 5. Audit event
      _ = auditService.sendSubmitDirectDebitPaymentPlan
    } yield {
      Redirect(routes.DirectDebitConfirmationController.onPageLoad())
    }
  }

  private def buildChrisSubmissionRequest(
                                           userAnswers: UserAnswers,
                                           ddiReference: String
                                         ): ChrisSubmissionRequest = {

    val totalAmountDue = userAnswers.get(TotalAmountDuePage)
    val paymentAmount = userAnswers.get(PaymentAmountPage)
    val regularPaymentAmount = userAnswers.get(RegularPaymentAmountPage)
    val paymentReference = userAnswers.get(PaymentReferencePage)
    val planStartDate = userAnswers.get(PlanStartDatePage)
    val planEndDate = userAnswers.get(PlanEndDatePage)
    val paymentDate = userAnswers.get(PaymentDatePage)
    val paymentFrequency = userAnswers.get(PaymentsFrequencyPage) // optional
    val yearEndAndMonth = userAnswers.get(YearEndAndMonthPage) // optional

    val bankDetailsWithAuddis = userAnswers.get(YourBankDetailsPage)
      .getOrElse(throw new Exception("Missing details: YourBankDetailsPage"))

    val bankName = userAnswers.get(BankDetailsBankNamePage)
      .getOrElse(throw new Exception("Missing details: BankDetailsBankNamePage"))

    val bankAddress = userAnswers.get(BankDetailsAddressPage)
      .getOrElse(throw new Exception("Missing details: BankDetailsAddressPage"))

    val calculationOpt: Option[PaymentPlanCalculation] =
      userAnswers.get(DirectDebitSourcePage) match {
        case Some(DirectDebitSource.TC) =>
          userAnswers.get(PaymentPlanTypePage) match {
            case Some(PaymentPlanType.TaxCreditRepaymentPlan) =>
              Some(calculateTaxCreditRepaymentPlan(userAnswers))
            case otherPlan =>
              logger.debug(s"No calculation needed for TC service with plan [$otherPlan]")
              None
          }
        case _ =>
          None
      }
    ChrisSubmissionRequest(
      serviceType = userAnswers.get(DirectDebitSourcePage).get,
      paymentPlanType = userAnswers.get(PaymentPlanTypePage).getOrElse(PaymentPlanType.SinglePayment),
      paymentFrequency = paymentFrequency,
      yourBankDetailsWithAuddisStatus = bankDetailsWithAuddis,
      auddisStatus = Some(bankDetailsWithAuddis.auddisStatus),
      planStartDate = planStartDate,
      planEndDate = planEndDate,
      paymentDate = paymentDate,
      yearEndAndMonth = yearEndAndMonth,
      bankDetails = YourBankDetailsWithAuddisStatus.toModelWithoutAuddisStatus(bankDetailsWithAuddis),
      bankDetailsAddress = bankAddress,
      bankName = bankName,
      ddiReferenceNo = ddiReference,
      paymentReference = paymentReference,
      totalAmountDue = totalAmountDue,
      paymentAmount = paymentAmount,
      regularPaymentAmount = regularPaymentAmount,
      calculation = calculationOpt
    )
  }


  private def calculateTaxCreditRepaymentPlan(userAnswers: UserAnswers): PaymentPlanCalculation = {
    val totalAmountDue = userAnswers.get(TotalAmountDuePage)
      .getOrElse(throw new Exception("Missing details: TotalAmountDuePage"))
    val planStartDate = userAnswers.get(PlanStartDatePage)
      .getOrElse(throw new Exception("Missing details: PlanStartDatePage"))

    val regularPaymentAmount = PaymentCalculations.calculateRegularPaymentAmount(
      totalAmountDueInput = totalAmountDue,
      totalNumberOfPayments = appConfig.tcTotalNumberOfPayments
    )

    val finalPaymentAmount = PaymentCalculations.calculateFinalPayment(
      totalAmountDue = totalAmountDue,
      regularPaymentAmount = BigDecimal(regularPaymentAmount),
      numberOfEqualPayments = appConfig.tcNumberOfEqualPayments
    )

    val secondPaymentDate = PaymentCalculations.calculateSecondPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset = appConfig.tcMonthsUntilSecondPayment
    )

    val penultimatePaymentDate = PaymentCalculations.calculatePenultimatePaymentDate(
      planStartDate = planStartDate.enteredDate,
      penultimateInstallmentOffset = appConfig.tcMonthsUntilPenultimatePayment
    )

    val finalPaymentDate = PaymentCalculations.calculateFinalPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset = appConfig.tcMonthsUntilFinalPayment
    )

    logger.debug(s"Regular Payment: £$regularPaymentAmount, Final Payment: £$finalPaymentAmount")
    logger.debug(s"Second: $secondPaymentDate, Penultimate: $penultimatePaymentDate, Final: $finalPaymentDate")

    PaymentPlanCalculation(
      regularPaymentAmount = Some(BigDecimal(regularPaymentAmount)),
      finalPaymentAmount = Some(finalPaymentAmount),
      secondPaymentDate = Some(secondPaymentDate),
      penultimatePaymentDate = Some(penultimatePaymentDate),
      finalPaymentDate = Some(finalPaymentDate)
    )
  }

}
