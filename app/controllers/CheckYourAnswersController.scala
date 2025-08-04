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

import config.FrontendAppConfig
import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{DirectDebitSource, UserAnswers}
import pages.{DirectDebitSourcePage, PlanStartDatePage, TotalAmountDuePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{DateTimeFormats, PaymentCalculations}
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView


class CheckYourAnswersController @Inject()  (
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            appConfig: FrontendAppConfig
                                          ) extends FrontendBaseController with I18nSupport with Logging{

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val directDebitSource = request.userAnswers.get(DirectDebitSourcePage)
      val showStartDate = if(directDebitSource.contains(DirectDebitSource.PAYE)){
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
          FinalPaymentAmountDueSummary.row(request.userAnswers),
        ).flatten
      )

      val currentDate = DateTimeFormats.formattedCurrentDate
      Ok(view(list, currentDate))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    paymentCalculation(request.userAnswers).getOrElse {
      logger.warn("Missing required answers for payment calculations")
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  }

  private def paymentCalculation(userAnswers: UserAnswers): Option[Result] = {
    for {
      totalAmountDue <- userAnswers.get(TotalAmountDuePage)
      planStartDate <- userAnswers.get(PlanStartDatePage)
    } yield {
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
        planStartDate = planStartDate,
        monthsOffset = appConfig.tcMonthsUntilSecondPayment
      )

      val penultimatePaymentDate = PaymentCalculations.calculatePenultimatePaymentDate(
        planStartDate = planStartDate,
        penultimateInstallmentOffset = appConfig.tcMonthsUntilPenultimatePayment
      )

      val finalPaymentDate = PaymentCalculations.calculateFinalPaymentDate(
        planStartDate = planStartDate,
        monthsOffset = appConfig.tcMonthsUntilFinalPayment
      )
      // perform further actions - these lines will be removed
      logger.debug(s"Regular Payment: £$regularPaymentAmount, Final Payment: £$finalPaymentAmount")
      logger.debug(s"Second: $secondPaymentDate, Penultimate: $penultimatePaymentDate, Final: $finalPaymentDate")

      Redirect(routes.DirectDebitConfirmationController.onPageLoad())
    }
  }

}
