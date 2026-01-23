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
import controllers.actions.*
import models.{NormalMode, PaymentPlanType}
import pages.{AmendConfirmRemovePlanEndDatePage, AmendPaymentAmountPage, ManagePaymentPlanTypePage, RegularPaymentAmountPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary}
import views.html.AmendingPaymentPlanView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendingPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: AmendingPaymentPlanView,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val messages: Messages = controllerComponents.messagesApi.preferred(request)
    if (!nddsService.amendPaymentPlanGuard(request.userAnswers)) {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
    } else {
      val planDetailsResponse = request.userAnswers
        .get(PaymentPlanDetailsQuery)
        .getOrElse(throw new RuntimeException("Missing plan details"))

      val planDetail = planDetailsResponse.paymentPlanDetails
      val isBudgetPlan = planDetail.planType == PaymentPlanType.BudgetPaymentPlan.toString
      val changeCall = if (isBudgetPlan) {
        routes.AmendRegularPaymentAmountController.onPageLoad(mode = NormalMode)
      } else {
        routes.AmendPaymentAmountController.onPageLoad(mode = NormalMode)
      }
      val hiddenChangeText = if (isBudgetPlan) {
        "amendRegularPaymentAmount.change.hidden"
      } else {
        "paymentAmount.change.hidden"
      }

      val amountRow = AmendPaymentAmountSummary
        .row(planDetail.planType, planDetail.scheduledPaymentAmount)
        .copy(actions =
          Some(
            Actions(items =
              Seq(
                ActionItem(
                  href               = changeCall.url,
                  content            = Text(messages("site.change")),
                  visuallyHiddenText = Some(messages(hiddenChangeText))
                )
              )
            )
          )
        )

      val dateRow: SummaryListRow =
        planDetail.planType match {
          case p if p == PaymentPlanType.SinglePaymentPlan.toString =>
            AmendPlanStartDateSummary
              .row(p, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)
              .copy(
                actions = Some(
                  Actions(
                    items = Seq(
                      ActionItem(
                        href               = routes.AmendPlanStartDateController.onPageLoad(NormalMode).url,
                        content            = Text(messages("site.change")),
                        visuallyHiddenText = Some(messages("amendPaymentAmount.change.hidden"))
                      )
                    )
                  )
                )
              )

          case p if p == PaymentPlanType.BudgetPaymentPlan.toString =>
            planDetail.scheduledPaymentEndDate match {
              case Some(endDate) =>
                AmendPlanEndDateSummary.row(
                  Some(endDate),
                  Constants.shortDateTimeFormatPattern,
                  true
                )
              case None =>
                AmendPlanEndDateSummary.addRow()
            }
          case _ =>
            SummaryListRow(key = Key(Text("")), value = Value(Text("")))
        }
      val paymentAmount = planDetail.scheduledPaymentAmount.get
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(RegularPaymentAmountPage, paymentAmount))
        updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, paymentAmount))
        updatedAnswers <- Future.fromTry(updatedAnswers.remove(AmendConfirmRemovePlanEndDatePage))
        _              <- sessionRepository.set(updatedAnswers)
      } yield {
        Ok(view(appConfig.hmrcHelplineUrl, amountRow, dateRow))
      }
    }
  }
}
