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
import models.{PaymentPlanType, UserAnswers}
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.AmendPaymentPlanUpdateView

import java.time.LocalDateTime
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
  view: AmendPaymentPlanUpdateView,
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
        val formattedStartDateLong = startDate.format(dateFormatLong)
        val directDebitDetails = paymentPlan.directDebitDetails
        val formattedSortCode = directDebitDetails.bankSortCode
          .map(sc => sc.grouped(2).mkString(" "))
          .getOrElse("")
        val submissionDate = paymentPlan.paymentPlanDetails.submissionDateTime
        val scheduledFrequency = paymentPlan.paymentPlanDetails.scheduledPaymentFrequency
        val paymentRef = paymentPlan.paymentPlanDetails.paymentReference
        val paymentList = buildSummaryRows(false, userAnswers, submissionDate, scheduledFrequency, paymentRef)

        Ok(
          view(
            appConfig.hmrcHelplineUrl,
            formatAmount(paymentAmount),
            formattedStartDateLong,
            directDebitRef,
            directDebitDetails.bankAccountName.getOrElse(""),
            directDebitDetails.bankAccountNumber.getOrElse(""),
            formattedSortCode,
            paymentList,
            paymentPlan.paymentPlanDetails.planType,
            routes.PaymentPlanDetailsController.onPageLoad()
          )
        )
      }

      maybeResult match {
        case Some(result) => Future.successful(result)
        case None =>
          logger.warn("Missing required values in user answers for amend payment plan")
          Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }

    } else {
      val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
    }
  }

  private def buildSummaryRows(showAllActionsFlag: Boolean,
                               userAnswers: UserAnswers,
                               dateSetup: LocalDateTime,
                               scheduledFrequency: Option[String],
                               paymentPlanReference: String
                              )(implicit
    messages: Messages
  ): SummaryList = {
    val planType: Option[String] = userAnswers.get(ManagePaymentPlanTypePage)
    val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
    val planStartDate = userAnswers.get(AmendPlanStartDatePage)
    val planEndDate = userAnswers.get(AmendPlanEndDatePage)

    planType match {
      case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
        SummaryListViewModel(
          Seq(
            PaymentReferenceSummary.row(paymentPlanReference),
            DateSetupSummary.row(dateSetup),
            AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, paymentAmount),
            AmendPlanStartDateSummary.row(PaymentPlanType.SinglePaymentPlan.toString, planStartDate, Constants.shortDateTimeFormatPattern)
          )
        )
      case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
        SummaryListViewModel(
          Seq(
            PaymentReferenceSummary.row(paymentPlanReference),
            DateSetupSummary.row(dateSetup),
            AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount),
            PaymentsFrequencySummary.row(scheduledFrequency),
            AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.shortDateTimeFormatPattern)
          ) ++
            planEndDate.map { date =>
              AmendPlanEndDateSummary.row(
                Some(date),
                Constants.shortDateTimeFormatPattern,
                showAllActionsFlag
              )
            }
        )
      case _ =>
        throw new RuntimeException("Invalid or missing planType in userAnswers")
    }

  }
}
