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
import models.{PaymentPlanType, UserAnswers}
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, PaymentReferenceSummary}
import views.html.RemoveSuspensionConfirmationView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.Future

class RemoveSuspensionConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveSuspensionConfirmationView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val userAnswers = request.userAnswers

      if (nddsService.suspendPaymentPlanGuard(userAnswers)) {
        val maybeResult = for {
          paymentReference <- userAnswers
                                .get(PaymentPlanDetailsQuery)
                                .map(_.paymentPlanDetails.paymentReference)
          paymentAmount <- userAnswers
                             .get(AmendPaymentAmountPage)
                             .orElse(
                               userAnswers
                                 .get(PaymentPlanDetailsQuery)
                                 .flatMap(_.paymentPlanDetails.scheduledPaymentAmount)
                             )
          startDate <- userAnswers
                         .get(AmendPlanStartDatePage)
                         .orElse(
                           userAnswers
                             .get(PaymentPlanDetailsQuery)
                             .flatMap(_.paymentPlanDetails.scheduledPaymentStartDate)
                         )
        } yield {

          val formattedRegPaymentAmount = formatAmount(paymentAmount)
          implicit val messages: Messages = messagesApi.preferred(request)

          val formattedStartDate =
            startDate.format(
              DateTimeFormatter.ofPattern(
                Constants.longDateTimeFormatPattern,
                messages.lang.locale
              )
            )

          val summaryRows: Seq[SummaryListRow] =
            buildSummaryRows(userAnswers, paymentReference)

          Ok(
            view(
              formattedRegPaymentAmount,
              formattedStartDate,
              summaryRows,
              routes.PaymentPlanDetailsController.onPageLoad()
            )
          )
        }

        maybeResult match {
          case Some(result) => Future.successful(result)
          case None =>
            logger.warn("Missing data in userAnswers for Payment Plan summary")
            Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
        }

      } else {
        val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: $planType")
        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }
    }

  private def buildSummaryRows(userAnswers: UserAnswers, paymentReference: String)(implicit messages: Messages): Seq[SummaryListRow] = {

    val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
    val planStartDate = userAnswers.get(AmendPlanStartDatePage)
    val planEndDate = userAnswers.get(AmendPlanEndDatePage)

    val baseRows = Seq(
      PaymentReferenceSummary.row(paymentReference),
      AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount),
      AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern)
    )

    planEndDate match {
      case Some(endDate) =>
        baseRows :+ AmendPlanEndDateSummary.row(Some(endDate), Constants.longDateTimeFormatPattern)
      case None =>
        baseRows
    }
  }
}
