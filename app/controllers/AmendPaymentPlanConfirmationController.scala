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
import models.*
import models.responses.PaymentPlanResponse
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.{ChrisSubmissionForAmendService, NationalDirectDebitService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendPaymentPlanConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPaymentPlanConfirmationView,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository,
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
        Future.successful(Ok(view(mode, buildRows(userAnswers, mode))))
      } else {
        val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }
    }
  }

  private def buildRows(userAnswers: UserAnswers, mode: Mode)(implicit
    messages: Messages
  ): Seq[SummaryListRow] = {
    userAnswers.get(ManagePaymentPlanTypePage) match {
      case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
        Seq(
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
        )

      case _ => // Budget Payment Plan
        Seq(
          AmendRegularPaymentAmountSummary.row(
            userAnswers.get(AmendPaymentAmountPage),
            showChange = true,
            changeCall = Some(routes.AmendRegularPaymentAmountController.onPageLoad(mode))
          ),
          userAnswers.get(AmendPlanEndDatePage) match {
            case Some(endDate) =>
              AmendPlanEndDateSummary.row(
                Some(endDate),
                Constants.shortDateTimeFormatPattern,
                true
              )
            case None =>
              AmendPlanEndDateSummary.addRow()
          }
        )
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val userAnswers = request.userAnswers

      userAnswers.get(ManagePaymentPlanTypePage) match {
        case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
          val amendPaymentDate = userAnswers.get(AmendPlanStartDatePage)
          handlePlanAmendment(userAnswers, amendPaymentDate, PaymentPlanType.SinglePaymentPlan.toString)

        case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
          val amendPlanEndDate = userAnswers.get(AmendPlanEndDatePage)
          handlePlanAmendment(userAnswers, amendPlanEndDate, PaymentPlanType.BudgetPaymentPlan.toString)

        case _ =>
          logger.warn("Missing payment plan type from session")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def handlePlanAmendment(
    userAnswers: UserAnswers,
    amendedDateOption: Option[LocalDate],
    planType: String
  )(implicit
    request: Request[?],
    ec: ExecutionContext
  ): Future[Result] = {

    val planDetailsOpt = userAnswers.get(PaymentPlanDetailsQuery)
    val amendedAmountOpt = userAnswers.get(AmendPaymentAmountPage)

    (planDetailsOpt, amendedAmountOpt) match {
      case (Some(planDetails), Some(amendedAmount)) =>
        val paymentDetails = planDetails.paymentPlanDetails

        // F27 check for any amendment
        def isNoChange(
          dbAmount: BigDecimal,
          dbStartDate: LocalDate,
          dbEndDate: Option[LocalDate]
        ): Boolean = {

          planType match {
            case PaymentPlanType.SinglePaymentPlan.toString =>
              amendedAmount == dbAmount &&
              amendedDateOption.contains(dbStartDate)

            case PaymentPlanType.BudgetPaymentPlan.toString =>
              amendedAmount == dbAmount &&
              amendedDateOption == dbEndDate

            case _ => false
          }
        }

        (paymentDetails.scheduledPaymentAmount, paymentDetails.scheduledPaymentStartDate, paymentDetails.scheduledPaymentEndDate) match {

          case (Some(dbAmount), Some(dbStartDate), Some(dbEndDate)) =>
            if (isNoChange(dbAmount, dbStartDate, Some(dbEndDate))) {
              Future.successful(Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()))
            } else {
              checkDuplicatePlan(userAnswers, amendedAmount, amendedDateOption, planType, dbStartDate)
            }

          case (Some(dbAmount), Some(dbStartDate), None) =>
            if (isNoChange(dbAmount, dbStartDate, None)) {
              Future.successful(Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()))
            } else {
              checkDuplicatePlan(userAnswers, amendedAmount, amendedDateOption, planType, dbStartDate)
            }

          case _ =>
            logger.warn("[handlePlanAmendment] Missing payment plan DB fields")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }

      case _ =>
        logger.warn(s"[handlePlanAmendment] Missing required fields for planType=$planType amendment")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  // F26 check for duplicate from RDS DB
  private def checkDuplicatePlan(userAnswers: UserAnswers,
                                 updatedAmount: BigDecimal,
                                 updatedDate: Option[LocalDate],
                                 planType: String,
                                 dbScheduledStartDate: LocalDate
                                )(implicit
    ec: ExecutionContext,
    request: Request[?]
  ): Future[Result] = {
    nddService.isDuplicatePaymentPlan(userAnswers).flatMap { duplicateResponse =>
      if (duplicateResponse.isDuplicate) {
        Future.successful(Redirect(routes.DuplicateWarningController.onPageLoad(NormalMode).url))
      } else {
        val updatedAnswers = for {
          updatedUa <- Future.fromTry(userAnswers.set(AmendPaymentPlanConfirmationPage, true))
          updatedUa <- Future.fromTry(updatedUa.set(AmendPaymentAmountPage, updatedAmount))
          updatedUa <- if (planType == PaymentPlanType.SinglePaymentPlan.toString) {
                         Future.fromTry(updatedUa.set(AmendPlanStartDatePage, updatedDate.get))
                       } else {
                         Future.fromTry(updatedUa.set(AmendPlanStartDatePage, dbScheduledStartDate))
                       }
          updatedUa <- if (updatedDate.isDefined) {
                         Future.fromTry(updatedUa.set(AmendPlanEndDatePage, updatedDate.get))
                       } else {
                         Future.successful(updatedUa)
                       }
        } yield updatedUa

        updatedAnswers.flatMap { finalUa =>
          sessionRepository.set(finalUa).flatMap { _ =>
            chrisService.submitToChris(
              ua              = finalUa,
              successRedirect = Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()),
              errorRedirect   = Redirect(routes.JourneyRecoveryController.onPageLoad())
            )
          }
        }
      }
    }
  }

}
