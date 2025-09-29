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

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DateSetupQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils.listHodServices
import viewmodels.checkAnswers.{AmendPlanStartDateSummary, *}
import views.html.PaymentPlanDetailsView

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class PaymentPlanDetailsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: PaymentPlanDetailsView,
                                              nddService: NationalDirectDebitService,
                                              sessionRepository: SessionRepository
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(PaymentReferenceQuery) match {
      case Some(paymentReference) =>
        nddService.getPaymentPlanDetails(paymentReference).flatMap { response =>
          val planDetail = response.paymentPlanDetails
          val directDebit = response.directDebitDetails
          val maybeSource: Option[DirectDebitSource] =
            listHodServices.find { case (_, v) => v.equalsIgnoreCase(planDetail.hodService) }.map(_._1)
          val frequency = PaymentsFrequency.fromString(planDetail.scheduledPaymentFrequency)

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPaymentPlanTypePage, planDetail.planType))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentPlanSourcePage, maybeSource.getOrElse("").toString))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(DateSetupQuery, planDetail.submissionDateTime))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, planDetail.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, planDetail.scheduledPaymentStartDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, planDetail.scheduledPaymentEndDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(PaymentReferenceQuery, planDetail.paymentReference))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(TotalAmountDuePage, planDetail.totalLiability))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(MonthlyPaymentAmountPage, planDetail.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(FinalPaymentAmountPage, planDetail.balancingPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(RegularPaymentAmountPage, planDetail.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(PaymentsFrequencyPage, frequency.get))
            cachedAnswers <- Future.fromTry(updatedAnswers.set(YourBankDetailsPage,
              YourBankDetailsWithAuddisStatus(
                accountHolderName = directDebit.bankAccountName,
                sortCode = directDebit.bankSortCode,
                accountNumber = directDebit.bankAccountNumber,
                auddisStatus = directDebit.auddisFlag,
                accountVerified = false
              )
            ))
            _ <- sessionRepository.set(cachedAnswers)
          } yield {
            val flag: Future[Boolean] = calculateShowAction(nddService, planDetail)
            val showActions = Await.result(flag, 5.seconds)
            val summaryRows: Seq[SummaryListRow] = buildSummaryRows(planDetail)
            Ok(view(planDetail.planType, paymentReference, showActions, summaryRows))
          }
        }
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(paymentReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentReferenceQuery, paymentReference))
      _ <- sessionRepository.set(updatedAnswers)
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
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate)
        )
      case PaymentPlanType.BudgetPaymentPlan.toString =>
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          TotalAmountDueSummary.row(planDetail.totalLiability),
          MonthlyPaymentAmountDueSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability),
          FinalPaymentAmountDueSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount),
          AmendSuspendDateSummary.row(planDetail.suspensionStartDate, true), //true for start
          AmendSuspendDateSummary.row(planDetail.suspensionEndDate, false), //false for end
        )
      case _ => //For Variable and Tax repayment plan
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService),
          DateSetupSummary.row(planDetail.submissionDateTime),
          TotalAmountDueSummary.row(planDetail.totalLiability),
          MonthlyPaymentAmountDueSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability),
          FinalPaymentAmountDueSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate),
        )
    }
  }

  private def calculateShowAction(nddService: NationalDirectDebitService, planDetail: PaymentPlanDetails)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    planDetail.planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        nddService.isTwoDaysPriorPaymentDate(planDetail.scheduledPaymentStartDate)

      case PaymentPlanType.BudgetPaymentPlan.toString | PaymentPlanType.VariablePaymentPlan.toString =>
        for {
          isTwoDaysBeforeStart <- nddService.isTwoDaysPriorPaymentDate(planDetail.scheduledPaymentStartDate)
          isThreeDaysBeforeEnd <- nddService.isThreeDaysPriorPlanEndDate(planDetail.scheduledPaymentEndDate)
        } yield isTwoDaysBeforeStart && isThreeDaysBeforeEnd

      case _ => Future.successful(false) //For TaxCredit repayment plan
    }
  }

}
