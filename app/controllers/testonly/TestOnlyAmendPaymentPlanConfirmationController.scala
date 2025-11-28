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

import controllers.actions.*
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import models.*
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import queries.PaymentPlanDetailsQuery
import services.{ChrisSubmissionForAmendService, NationalDirectDebitService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers
import viewmodels.checkAnswers.*
import views.html.testonly.TestOnlyAmendPaymentPlanConfirmationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPaymentPlanConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPaymentPlanConfirmationView,
  nddService: NationalDirectDebitService,
  chrisService: ChrisSubmissionForAmendService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    val alreadyConfirmed: Boolean =
      userAnswers.get(AmendPaymentPlanConfirmationPage).contains(true)

    if (alreadyConfirmed) {
      logger.warn("Attempt to load Cancel this payment plan confirmation; redirecting to Page Not Found.")
      Future.successful(Redirect(routes.BackSubmissionController.onPageLoad()))
    } else {
      if (nddService.amendPaymentPlanGuard(userAnswers)) {
        val (rows, backLink) = buildRows(userAnswers, mode)

        Future.successful(Ok(view(mode, rows, backLink)))
      } else {
        val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }

  private def buildRows(userAnswers: UserAnswers, mode: Mode)(implicit
    messages: Messages
  ): (Seq[SummaryListRow], Call) = {
    userAnswers.get(ManagePaymentPlanTypePage) match {
      case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
        (Seq(
           AmendPaymentAmountSummary.row(
             PaymentPlanType.SinglePaymentPlan.toString,
             userAnswers.get(AmendPaymentAmountPage),
             true
           ),
           AmendPlanStartDateSummary.row(
             PaymentPlanType.SinglePaymentPlan.toString,
             userAnswers.get(AmendPlanStartDatePage),
             Constants.shortDateTimeFormatPattern,
             true
           )
         ),
         userAnswers.get(AmendPaymentAmountPage) match {
           case Some(_) => testOnlyRoutes.TestOnlyAmendPaymentAmountController.onPageLoad(mode)
           case _       => testOnlyRoutes.TestOnlyAmendPlanStartDateController.onPageLoad(mode)
         }
        )
      case _ => // Budget Payment Plan
        (Seq(
           AmendPaymentAmountSummary.row(
             PaymentPlanType.BudgetPaymentPlan.toString,
             userAnswers.get(AmendPaymentAmountPage),
             true
           ), // TODO - replace with AP1a TestOnly Amend RegularPaymentAmount
           AmendPlanEndDateSummary.row(
             userAnswers.get(AmendPlanEndDatePage),
             Constants.shortDateTimeFormatPattern,
             true
           ) // TODO - replace with AP1c TestOnly AmendPlanEndDate
         ),
         userAnswers.get(AmendPlanEndDatePage) match {
           case Some(_) => routes.AmendPlanEndDateController.onPageLoad(mode) // TODO - replace with TestOnly Amend plan end date controller
           case _ => routes.RegularPaymentAmountController.onPageLoad(mode) // TODO - replace with TestOnly Amend regular payment amount controller
         }
        )
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val userAnswers = request.userAnswers
      val planDetailsQuery = userAnswers.get(PaymentPlanDetailsQuery)

      userAnswers.get(ManagePaymentPlanTypePage) match {
        case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
          val amendPaymentDate = userAnswers.get(AmendPaymentDatePage)
          val amendPaymentAmount = userAnswers.get(AmendPaymentAmountPage)
          (planDetailsQuery, amendPaymentAmount, amendPaymentDate) match {
            case (Some(planDetails), Some(amendedAmount), Some(amendedDate)) =>
              val dbAmount = planDetails.paymentPlanDetails.scheduledPaymentAmount.get
              val dbStartDate = planDetails.paymentPlanDetails.scheduledPaymentStartDate.get

              val noChange = amendedAmount == dbAmount && amendedDate == dbStartDate
              if (noChange) { // F27 amendment check
                Future.successful(Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad()))
              } else {
                // F26 duplicate check
                checkDuplicatePlan(userAnswers)
              }
            case _ =>
              logger.warn("Missing Amend payment amount and/or amend payment date")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
          val amendPlanEndDate = userAnswers.get(AmendPlanEndDatePage)
          val amendRegularPaymentAmount = userAnswers.get(AmendPaymentAmountPage)
          (planDetailsQuery, amendRegularPaymentAmount, amendPlanEndDate) match {
            case (Some(planDetails), Some(amendedRegAmount), Some(amendedEndDate)) =>
              val dbAmount = planDetails.paymentPlanDetails.scheduledPaymentAmount.get
              val dbEndDate = planDetails.paymentPlanDetails.scheduledPaymentEndDate.get

              val noChange = amendedRegAmount == dbAmount && amendedEndDate == dbEndDate
              if (noChange) { // F27 amendment check
                Future.successful(Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad()))
              } else {
                // F26 duplicate check
                checkDuplicatePlan(userAnswers)
              }
            case _ =>
              logger.warn("Missing Amend payment amount and/or amend plan end date")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        case _ =>
          logger.warn("Missing Amend regular or payment amount and/or amend payment date or plan end date from session")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def checkDuplicatePlan(userAnswers: UserAnswers)(implicit ec: ExecutionContext, request: Request[?]): Future[Result] = {
    nddService.isDuplicatePaymentPlan(userAnswers).flatMap { duplicateResponse =>
      if (duplicateResponse.isDuplicate) {
        Future.successful(Redirect(testOnlyRoutes.TestOnlyDuplicateWarningController.onPageLoad(NormalMode).url))
      } else {
        chrisService.submitToChris(
          ua              = userAnswers,
          successRedirect = Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad()),
          errorRedirect   = Redirect(routes.JourneyRecoveryController.onPageLoad())
        )
      }
    }
  }

}
