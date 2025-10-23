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
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanReferenceQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanUpdateView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.Future

class AmendPaymentPlanUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPaymentPlanUpdateView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    if (nddsService.amendPaymentPlanGuard(userAnswers)) {
      val maybeResult = for {
        paymentPlanReference <- userAnswers.get(PaymentPlanReferenceQuery)
        paymentAmount        <- userAnswers.get(AmendPaymentAmountPage)
        startDate            <- userAnswers.get(AmendPlanStartDatePage)
      } yield {
        val formattedRegPaymentAmount = formatAmount(paymentAmount)
        val formattedStartDate =
          startDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val summaryRows: Seq[SummaryListRow] = buildSummaryRows(userAnswers, paymentPlanReference)

        Ok(view(paymentPlanReference, formattedRegPaymentAmount, formattedStartDate, summaryRows))
      }
      maybeResult match {
        case Some(result) => Future.successful(result)
        case None =>
          logger.warn("Missing required values in user answers for amend payment plan")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    } else {
      logger.error(s"NDDS Payment Plan Guard check failed")
      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def buildSummaryRows(userAnswers: UserAnswers, paymentPlanReference: String)(implicit messages: Messages): Seq[SummaryListRow] = {

    val planTypeOpt = userAnswers.get(ManagePaymentPlanTypePage)
    val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
    val planStartDate = userAnswers.get(AmendPlanStartDatePage)
    val planEndDate = userAnswers.get(AmendPlanEndDatePage)

    planTypeOpt match {
      case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
        Seq(
          PaymentReferenceSummary.row(paymentPlanReference),
          AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, paymentAmount),
          AmendPlanStartDateSummary.row(PaymentPlanType.SinglePaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern)
        )
      case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
        Seq(
          PaymentReferenceSummary.row(paymentPlanReference),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern),
          AmendPlanEndDateSummary.row(planEndDate, Constants.longDateTimeFormatPattern)
        )
      case _ => throw new IllegalStateException("Plan type is missing or invalid")
    }
  }
}
