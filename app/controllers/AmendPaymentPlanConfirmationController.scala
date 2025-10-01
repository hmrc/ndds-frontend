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
import models.responses.PaymentPlanDetails
import models.{Mode, PaymentPlanType, UserAnswers}
import pages.*
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AmendPaymentPlanConfirmationController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       requireData: DataRequiredAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: AmendPaymentPlanConfirmationView,
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val userAnswers = request.userAnswers

      userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(response) =>
          val planDetail = response.paymentPlanDetails
          val directDebitDetails = response.directDebitDetails

          val (rows, backLink) = buildRows(userAnswers, planDetail, mode)

          for {
            directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
            paymentPlanReference <- Future.fromTry(Try(userAnswers.get(PaymentPlanReferenceQuery).get))
            planType <- Future.fromTry(Try(userAnswers.get(AmendPaymentPlanTypePage).get))
          } yield {
            Ok(view(
              mode,
              paymentPlanReference,
              directDebitReference,
              directDebitDetails.bankSortCode.getOrElse(""),
              directDebitDetails.bankAccountNumber.getOrElse(""),
              rows,
              backLink
            ))
          }

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    Future.successful(Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()))
  }

  private def buildRows(userAnswers: UserAnswers, paymentPlan: PaymentPlanDetails, mode: Mode) (implicit messages: Messages): (Seq[SummaryListRow], Call) =
    userAnswers.get(AmendPaymentPlanTypePage) match {
      case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
        (Seq(
          AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
          AmendPaymentPlanSourceSummary.row(paymentPlan.hodService),
          //DateSetupSummary.row(paymentPlan.submissionDateTime),
          TotalAmountDueSummary.row(paymentPlan.totalLiability),
          MonthlyPaymentAmountSummary.row(paymentPlan.scheduledPaymentAmount, paymentPlan.totalLiability),
          FinalPaymentAmountSummary.row(paymentPlan.balancingPaymentAmount, paymentPlan.totalLiability),
          PaymentsFrequencySummary.row(paymentPlan.scheduledPaymentFrequency),
          AmendPlanStartDateSummary.row(
            PaymentPlanType.BudgetPaymentPlan.toString,
            userAnswers.get(AmendPlanStartDatePage).getOrElse(LocalDate.now())
          ),
          AmendPaymentAmountSummary.row(
            PaymentPlanType.BudgetPaymentPlan.toString,
            userAnswers.get(AmendPaymentAmountPage),
            true
          ),
          AmendPlanEndDateSummary.row(
            userAnswers.get(AmendPlanEndDatePage).getOrElse(LocalDate.now()),
            true
          )
        ), routes.AmendPlanEndDateController.onPageLoad(mode))

      case _ =>
        (Seq(
          AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
          AmendPaymentPlanSourceSummary.row(paymentPlan.hodService),
          DateSetupSummary.row(paymentPlan.submissionDateTime),
          AmendPaymentAmountSummary.row(
            PaymentPlanType.SinglePaymentPlan.toString,
            userAnswers.get(AmendPaymentAmountPage),
            true
          ),
          AmendPlanStartDateSummary.row(
            PaymentPlanType.SinglePaymentPlan.toString,
            userAnswers.get(AmendPlanStartDatePage).getOrElse(LocalDate.now()),
            true
          )
        ), routes.AmendPlanStartDateController.onPageLoad(mode))
    }

}
