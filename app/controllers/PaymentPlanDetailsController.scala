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
import models.responses.{AdvanceNoticeResponse, PaymentPlanDetails}
import pages.*
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AdvanceNoticeResponseQuery, DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.PaymentPlanDetailsView

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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

          val advanceNoticeResponse = request.userAnswers.get(AdvanceNoticeResponseQuery)
          val isAdvanceNoticePresent = advanceNoticeResponse.isDefined
          println("Advanmce " + advanceNoticeResponse.flatMap(_.totalAmount))

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanDetailsQuery, response))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(ManagePaymentPlanTypePage, planDetail.planType))
            updatedAnswers <- planDetail.scheduledPaymentAmount match {
                                case Some(amount) => Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, amount))
                                case _            => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- (planDetail.suspensionStartDate, planDetail.suspensionEndDate) match {
                                case (Some(startDate), Some(endDate)) =>
                                  Future.fromTry(updatedAnswers.set(SuspensionPeriodRangeDatePage, SuspensionPeriodRange(startDate, endDate)))
                                case _ => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- planDetail.scheduledPaymentStartDate match {
                                case Some(paymentStartDate) => Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, paymentStartDate))
                                case _                      => Future.successful(updatedAnswers)
                              }
            updatedAnswers <- planDetail.scheduledPaymentEndDate match {
                                case Some(paymentEndDate) => Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, paymentEndDate))
                                case _                    => Future.successful(updatedAnswers)
                              }
            updatedAnswers     <- cleanseSessionPages(updatedAnswers)
            showAllActionsFlag <- calculateShowAction(nddService, planDetail)
            _                  <- sessionRepository.set(updatedAnswers)
          } yield {
            val showAmendLink = isAmendLinkVisible(showAllActionsFlag, planDetail)
            val showCancelLink = isCancelLinkVisible(showAllActionsFlag, planDetail)
            val showSuspendLink = isSuspendLinkVisible(showAllActionsFlag, planDetail)
            val summaryRows: Seq[SummaryListRow] = buildSummaryRows(planDetail)
            val isSuspensionActive = isSuspendPeriodActive(planDetail)

            val formattedSuspensionStartDate = planDetail.suspensionStartDate
              .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
              .getOrElse("")

            val formattedSuspensionEndDate = planDetail.suspensionEndDate
              .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
              .getOrElse("")

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
            val formattedTotalAmount: String = advanceNoticeResponse
              .flatMap(_.totalAmount)
              .map(amount => currencyFormat.format(amount.bigDecimal))
              .getOrElse("")

            val formattedDueDate = advanceNoticeResponse
              .flatMap(_.dueDate)
              .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
              .getOrElse("")

            Ok(
              view(
                planDetail.planType,
                paymentPlanReference,
                showAmendLink,
                showCancelLink,
                showSuspendLink,
                isSuspensionActive,
                formattedSuspensionStartDate,
                formattedSuspensionEndDate,
                summaryRows,
                isAdvanceNoticePresent,
                formattedTotalAmount,
                formattedDueDate,
                routes.AdvanceNoticeController.onPageLoad()
              )
            )
          }
        }

      case _ =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(directDebitReference: String, paymentPlanReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      // TODO: Correction as hardcoding now because of stub data
      // val isVariablePlan = nddService.isVariablePaymentPlan(request.userAnswers)
      val isVariablePlan = true
      getAdvanceNoticeData(directDebitReference, paymentPlanReference, isVariablePlan).flatMap { (advanceNoticeResponse, isAdvanceNoticePresent) =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(DirectDebitReferenceQuery, directDebitReference))
          updatedAnswers <- Future.fromTry(updatedAnswers.set(PaymentPlanReferenceQuery, paymentPlanReference))
          updatedAnswers <- Future.fromTry(updatedAnswers.set(AdvanceNoticeResponseQuery, advanceNoticeResponse))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(routes.PaymentPlanDetailsController.onPageLoad())
      }
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
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)
        ) ++
          planDetail.scheduledPaymentEndDate.fold(Seq.empty[SummaryListRow]) { scheduledPaymentEndDate =>
            Seq(AmendPlanEndDateSummary.row(Some(scheduledPaymentEndDate), Constants.shortDateTimeFormatPattern))
          }
          ++
          (if (isSuspendPeriodActive(planDetail)) {
             Seq(SuspensionPeriodRangeDateSummary.row(planDetail.suspensionStartDate, planDetail.suspensionEndDate))
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

  private def calculateShowAction(nddService: NationalDirectDebitService, planDetail: PaymentPlanDetails)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] = {
    planDetail.planType match {
      case PaymentPlanType.SinglePaymentPlan.toString | PaymentPlanType.VariablePaymentPlan.toString =>
        planDetail.scheduledPaymentStartDate match {
          case Some(startDate) => nddService.isTwoDaysPriorPaymentDate(startDate)
          case None            => Future.successful(true)
        }
      case PaymentPlanType.BudgetPaymentPlan.toString =>
        for {
          isThreeDaysBeforeEnd <- planDetail.scheduledPaymentEndDate match {
                                    case Some(endDate) => nddService.isThreeDaysPriorPlanEndDate(endDate)
                                    case _             => Future.successful(true)
                                  }
        } yield isThreeDaysBeforeEnd
      case PaymentPlanType.TaxCreditRepaymentPlan.toString => Future.successful(false)
    }
  }

  private def cleanseSessionPages(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      updatedUserAnswers <- Future.fromTry(userAnswers.remove(CancelPaymentPlanPage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(DuplicateWarningPage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(RemovingThisSuspensionPage))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers

  private def isAmendLinkVisible(showAllActionsFlag: Boolean, planDetail: PaymentPlanDetails): Boolean = {
    val amendableTypes = Set(
      PaymentPlanType.SinglePaymentPlan.toString,
      PaymentPlanType.BudgetPaymentPlan.toString
    )

    showAllActionsFlag && amendableTypes.contains(planDetail.planType)
  }

  private def isCancelLinkVisible(showAllActionsFlag: Boolean, planDetail: PaymentPlanDetails): Boolean = {
    showAllActionsFlag && planDetail.planType != PaymentPlanType.TaxCreditRepaymentPlan.toString
  }

  private def isSuspendLinkVisible(showAllActionsFlag: Boolean, planDetail: PaymentPlanDetails): Boolean = {
    planDetail.planType match {
      case planType if planType == PaymentPlanType.BudgetPaymentPlan.toString =>
        showAllActionsFlag && !isSuspendPeriodActive(planDetail)
      case _ => false
    }
  }

  private def isSuspendPeriodActive(planDetail: PaymentPlanDetails): Boolean = {
    (for {
      suspensionEndDate <- planDetail.suspensionEndDate
    } yield !LocalDate.now().isAfter(suspensionEndDate)).getOrElse(false)
  }

  private def getAdvanceNoticeData(
    directDebitReference: String,
    paymentPlanReference: String,
    isVariablePlan: Boolean
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[(AdvanceNoticeResponse, Boolean)] = {
    val advanceNoticeResult: Future[AdvanceNoticeResponse] =
      if (isVariablePlan) {
        nddService
          .isAdvanceNoticePresent(directDebitReference, paymentPlanReference)
          .recover { case _ =>
            AdvanceNoticeResponse(None, None)
          }
      } else {
        Future.successful(AdvanceNoticeResponse(None, None))
      }
    advanceNoticeResult.map { advanceNoticeResponse =>
      val isAdvanceNoticePresent =
        advanceNoticeResponse.totalAmount.isDefined || advanceNoticeResponse.dueDate.isDefined

      (advanceNoticeResponse, isAdvanceNoticePresent)
    }
  }
}
