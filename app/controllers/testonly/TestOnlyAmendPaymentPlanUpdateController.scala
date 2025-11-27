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
import controllers.routes
import models.{PaymentPlanType, UserAnswers}
import models.responses.PaymentPlanDetails
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, DateSetupSummary, PaymentReferenceSummary, PaymentsFrequencySummary}
import views.html.testonly.TestOnlyAmendPaymentPlanUpdateView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.Future

class TestOnlyAmendPaymentPlanUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPaymentPlanUpdateView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers

    if (nddsService.amendPaymentPlanGuard(userAnswers)) {

      val maybeResult = for {
        paymentPlan    <- userAnswers.get(PaymentPlanDetailsQuery)
        paymentAmount  <- userAnswers.get(AmendPaymentAmountPage)
        startDate      <- userAnswers.get(AmendPlanStartDatePage)
        directDebitRef <- userAnswers.get(DirectDebitReferenceQuery)
        paymentPlanRef <- userAnswers.get(PaymentPlanReferenceQuery)
      } yield {
        val dateFormatLong = DateTimeFormatter.ofPattern("d MMMM yyyy")
        val dateFormatShort = DateTimeFormatter.ofPattern("d MMM yyyy")

        val formattedStartDateLong = startDate.format(dateFormatLong)
        val formattedStartDateShort = startDate.format(dateFormatShort)
        val formattedSubmissionDate = paymentPlan.paymentPlanDetails.submissionDateTime.format(dateFormatShort)
        val directDebitDetails = paymentPlan.directDebitDetails
        val formattedSortCode = directDebitDetails.bankSortCode
          .map(sc => sc.grouped(2).mkString(" "))
          .getOrElse("")

        Ok(
          view(
            appConfig.hmrcHelplineUrl,
            formatAmount(paymentAmount),
            formattedStartDateLong,
            directDebitRef,
            directDebitDetails.bankAccountName.getOrElse(""),
            directDebitDetails.bankAccountNumber.getOrElse(""),
            formattedSortCode,
            paymentPlan.paymentPlanDetails.paymentReference,
            formattedSubmissionDate,
            formattedStartDateShort,
            controllers.routes.PaymentPlanDetailsController.onPageLoad()
          )
        )
      }

      maybeResult match {
        case Some(result) => Future.successful(result)
        case None =>
          logger.warn("Missing required values in user answers for amend payment plan")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }

    } else {
      val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
    }
  }

  private def buildSummaryRows(showAllActionsFlag: Boolean, userAnswers: UserAnswers, planDetail: PaymentPlanDetails, paymentPlanReference: String)(
    implicit messages: Messages
  ): Seq[SummaryListRow] = {
    val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
    val planStartDate = userAnswers.get(AmendPlanStartDatePage)
    val planEndDate = userAnswers.get(AmendPlanEndDatePage)

    planDetail.planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        Seq(
          PaymentReferenceSummary.row(paymentPlanReference),
          DateSetupSummary.row(planDetail.submissionDateTime),
          AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, paymentAmount),
          AmendPlanStartDateSummary.row(PaymentPlanType.SinglePaymentPlan.toString, planStartDate, Constants.shortDateTimeFormatPattern)
        )
      case PaymentPlanType.BudgetPaymentPlan.toString =>
        Seq(
          PaymentReferenceSummary.row(paymentPlanReference),
          DateSetupSummary.row(planDetail.submissionDateTime),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.shortDateTimeFormatPattern),
          AmendPlanEndDateSummary.row(planEndDate, Constants.shortDateTimeFormatPattern, showAllActionsFlag)
        )
    }

  }
}
