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
import models.responses.PaymentPlanDetails
import pages.*
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.PaymentPlanDetailsView

import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class PaymentPlanDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentPlanDetailsView,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    (request.userAnswers.get(DirectDebitReferenceQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
      case (Some(directDebitReference), Some(paymentPlanReference)) =>
        nddService.getPaymentPlanDetails(directDebitReference, paymentPlanReference).flatMap { response =>
          val planDetail = response.paymentPlanDetails
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanDetailsQuery, response))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(ManagePaymentPlanTypePage, planDetail.planType))
            updatedAnswers <- planDetail.scheduledPaymentAmount match {
                                case Some(amount) => Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, amount))
                                case None         => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- (planDetail.suspensionStartDate, planDetail.suspensionEndDate) match {
                                case (Some(startDate), Some(endDate)) =>
                                  Future.fromTry(updatedAnswers.set(SuspensionPeriodRangeDatePage, SuspensionPeriodRange(startDate, endDate)))
                                case _ => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- planDetail.scheduledPaymentStartDate match {
                                case Some(startDate) => Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, startDate))
                                case None            => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- planDetail.scheduledPaymentEndDate match {
                                case Some(endDate) => Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, endDate))
                                case None          => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- cleanseCancelPaymentPlanPage(updatedAnswers)
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            val flag: Future[Boolean] = calculateShowAction(nddService, planDetail)
            val showActions = Await.result(flag, 5.seconds)
            val summaryRows: Seq[SummaryListRow] = buildSummaryRows(planDetail)
            Ok(view(planDetail.planType, paymentPlanReference, showActions, summaryRows))
          }
        }
      case _ =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(paymentPlanReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanReferenceQuery, paymentPlanReference))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Redirect(routes.PaymentPlanDetailsController.onPageLoad())
    }

  private def buildSummaryRows(planDetail: PaymentPlanDetails)(implicit messages: Messages): Seq[SummaryListRow] = {
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
          TotalAmountDueSummary.row(planDetail.totalLiability),
          MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability),
          FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount),
          AmendSuspendDateSummary.row(planDetail.suspensionStartDate, true), // true for start
          AmendSuspendDateSummary.row(planDetail.suspensionEndDate, false) // false for end
        )
      case _ => // For Variable and Tax repayment plan
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

  private def calculateShowAction(nddService: NationalDirectDebitService, planDetail: PaymentPlanDetails)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] = {
    planDetail.planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        planDetail.scheduledPaymentStartDate match {
          case Some(startDate) => nddService.isTwoDaysPriorPaymentDate(startDate)
          case None            => Future.successful(true)
        }
      case PaymentPlanType.BudgetPaymentPlan.toString =>
        planDetail.scheduledPaymentEndDate match {
          case Some(startDate) => nddService.isThreeDaysPriorPlanEndDate(startDate)
          case None            => Future.successful(true)
        }
      case PaymentPlanType.VariablePaymentPlan.toString =>
        for {
          isTwoDaysBeforeStart <- planDetail.scheduledPaymentStartDate match {
                                    case Some(startDate) => nddService.isTwoDaysPriorPaymentDate(startDate)
                                    case None            => Future.successful(true)
                                  }
          isThreeDaysBeforeEnd <- planDetail.scheduledPaymentEndDate match {
                                    case Some(endDate) => nddService.isThreeDaysPriorPlanEndDate(endDate)
                                    case None          => Future.successful(true)
                                  }
        } yield isTwoDaysBeforeStart && isThreeDaysBeforeEnd

      case _ => Future.successful(false) // For TaxCredit repayment plan
    }
  }

  private def cleanseCancelPaymentPlanPage(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      updatedUserAnswers <- Future.fromTry(userAnswers.remove(CancelPaymentPlanPage))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers

}
