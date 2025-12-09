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
import models.responses.PaymentPlanDetails
import pages.ManagePaymentPlanTypePage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.PaymentPlanCancelledView

import java.time.LocalDate
import javax.inject.Inject

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
      userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(planDetails) =>
          val paymentPlanDetails = planDetails.paymentPlanDetails
          val rows = buildRows(paymentPlanDetails)

          Ok(view(paymentPlanDetails.paymentReference, routes.YourDirectDebitInstructionsController.onPageLoad(), rows))
        case _ =>
          Redirect(routes.SystemErrorController.onPageLoad())
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot cancel this plan type: $planType")
      Redirect(routes.SystemErrorController.onPageLoad())
    }

  }

  private def isSuspendPeriodActive(planDetail: PaymentPlanDetails): Boolean = {
    (for {
      suspensionEndDate <- planDetail.suspensionEndDate
    } yield !LocalDate.now().isAfter(suspensionEndDate)).getOrElse(false)
  }

  private def buildRows(planDetail: PaymentPlanDetails)(implicit messages: Messages): Seq[SummaryListRow] = {
    planDetail.planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)
        )
      case PaymentPlanType.BudgetPaymentPlan.toString =>
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)
        ) ++
          planDetail.scheduledPaymentEndDate.fold(Seq.empty[SummaryListRow]) { scheduledPaymentEndDate =>
            Seq(AmendPlanEndDateSummary.row(Some(scheduledPaymentEndDate), Constants.shortDateTimeFormatPattern))
          }
          ++
          (if (isSuspendPeriodActive(planDetail)) {
             Seq(SuspensionPeriodRangeDateSummary.row(planDetail.suspensionStartDate, planDetail.suspensionEndDate, showActions = false))
           } else {
             Seq.empty
           })
      case PaymentPlanType.VariablePaymentPlan.toString =>
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)
        )
      case _ =>
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          TotalAmountDueSummary.row(planDetail.totalLiability),
          MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability),
          FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)
        )
    }
  }

}
