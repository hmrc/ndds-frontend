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
import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import pages.{CheckYourAnswerPage, DirectDebitSourcePage, PaymentPlanTypePage, PaymentReferencePage, PlanStartDatePage, TotalAmountDuePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
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
      reference <- nddService.generateNewDdiReference(
        request.userAnswers.get(PaymentReferencePage).getOrElse(throw new Exception("Missing details: PaymentReferencePage"))
      )
      res = paymentCalculation(request.userAnswers)
      updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckYourAnswerPage, reference))
      _ <- sessionRepository.set(updatedAnswers)
      _ = auditService.sendSubmitDirectDebitPaymentPlan
    } yield {
      res
    }
  }

  private def paymentCalculation(userAnswers: UserAnswers): Result = {
    userAnswers.get(DirectDebitSourcePage).getOrElse(throw new Exception("Missing details: DirectDebitSourcePage")) match {
      case DirectDebitSource.TC =>
        userAnswers.get(PaymentPlanTypePage).getOrElse(throw new Exception("Missing details: PaymentPlanTypePage")) match {
          case PaymentPlanType.TaxCreditRepaymentPlan =>
            calculateTaxCreditRepaymentPlan(userAnswers)
          case otherPlan =>
            logger.debug(s"No calculation needed for TC service with plan [$otherPlan]")
            Redirect(routes.DirectDebitConfirmationController.onPageLoad())
        }
      // will add next steps  and update
      case _ =>
        Redirect(routes.DirectDebitConfirmationController.onPageLoad())
    }
  }

  private def calculateTaxCreditRepaymentPlan(userAnswers: UserAnswers): Result = {
    val totalAmountDue = userAnswers.get(TotalAmountDuePage).getOrElse(throw new Exception("Missing details: TotalAmountDuePage"))
    val planStartDate = userAnswers.get(PlanStartDatePage).getOrElse(throw new Exception("Missing details: PlanStartDatePage"))

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

    Redirect(routes.DirectDebitConfirmationController.onPageLoad())
  }

}
