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

package controllers.testonly

import config.FrontendAppConfig
import controllers.actions.*
import controllers.testonly.routes as testOnlyRoutes
import models.{NormalMode, PaymentPlanType}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.Constants
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanStartDateSummary, SuspensionPeriodRangeDateSummary}
import viewmodels.testonly.TestOnlyPlanEndDateSummary
import views.html.testonly.TestOnlyAmendingPaymentPlanView

import javax.inject.Inject

class TestOnlyAmendingPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendingPaymentPlanView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val planDetails =
        request.userAnswers
          .get(PaymentPlanDetailsQuery)
          .getOrElse(sys.error("Missing plan details"))
          .paymentPlanDetails

      val amountRow =
        AmendPaymentAmountSummary
          .row(planDetails.planType, planDetails.scheduledPaymentAmount)
          .copy(
            actions = Some(
              Actions(
                items = Seq(
                  ActionItem(
                    href = testOnlyRoutes.TestOnlyAmendPaymentAmountController
                      .onPageLoad(NormalMode)
                      .url,
                    content            = Text("Change"),
                    visuallyHiddenText = Some("payment amount")
                  )
                )
              )
            )
          )

      val dateRow: SummaryListRow =
        planDetails.planType match {

          // SINGLE PLAN
          case p if p == PaymentPlanType.SinglePaymentPlan.toString =>
            AmendPlanStartDateSummary
              .row(
                p,
                planDetails.scheduledPaymentStartDate,
                Constants.shortDateTimeFormatPattern
              )
              .copy(
                actions = Some(
                  Actions(
                    items = Seq(
                      ActionItem(
                        href = testOnlyRoutes.TestOnlyAmendPlanStartDateController
                          .onPageLoad(NormalMode)
                          .url,
                        content            = Text("Change"),
                        visuallyHiddenText = Some("payment date")
                      )
                    )
                  )
                )
              )

          // BUDGET PLAN
          case p if p == PaymentPlanType.BudgetPaymentPlan.toString =>
            planDetails.scheduledPaymentEndDate match {
              case Some(endDate) =>
                TestOnlyPlanEndDateSummary.row(endDate)
              case None =>
                TestOnlyPlanEndDateSummary.addRow()
            }

          case _ =>
            SummaryListRow(key = Key(Text("")), value = Value(Text("")))
        }

//      val summaryRows: Seq[SummaryListRow] = Seq(amountRow, dateRow)
      Ok(
        view(
          appConfig.hmrcHelplineUrl,
          amountRow,
          dateRow
        )
      )
    }
}
