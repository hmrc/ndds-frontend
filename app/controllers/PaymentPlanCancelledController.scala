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

import controllers.actions.*
import models.PaymentPlanType

import javax.inject.Inject
import models.responses.PaymentPlanDetails
import pages.ManagePaymentPlanTypePage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import views.html.PaymentPlanCancelledView

class PaymentPlanCancelledController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentPlanCancelledView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val userAnswers = request.userAnswers

    if (nddService.isPaymentPlanCancellable(userAnswers)) {
      (userAnswers.get(PaymentPlanDetailsQuery), userAnswers.get(PaymentPlanReferenceQuery)) match {
        case (Some(planDetails), Some(paymentPlanReference)) =>
          val paymentPlanDetails = planDetails.paymentPlanDetails
          val rows = buildRows(paymentPlanDetails)

          Ok(view(paymentPlanReference, routes.DirectDebitSummaryController.onPageLoad(), rows))
        case _ =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot cancel this plan type: $planType")
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

  }

  private def buildRows(paymentPlanDetails: PaymentPlanDetails)(implicit messages: Messages): Seq[SummaryListRow] = {
    if (paymentPlanDetails.planType == PaymentPlanType.BudgetPaymentPlan.toString) {
      Seq(
        AmendPaymentPlanTypeSummary.row(paymentPlanDetails.planType),
        AmendPaymentPlanSourceSummary.row(paymentPlanDetails.hodService),
        DateSetupSummary.row(paymentPlanDetails.submissionDateTime),
        PaymentsFrequencySummary.row(paymentPlanDetails.scheduledPaymentFrequency),
        AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)
      )
    } else {
      Seq(
        AmendPaymentPlanTypeSummary.row(paymentPlanDetails.planType),
        AmendPaymentPlanSourceSummary.row(paymentPlanDetails.hodService),
        DateSetupSummary.row(paymentPlanDetails.submissionDateTime),
        AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)
      )
    }
  }

}
