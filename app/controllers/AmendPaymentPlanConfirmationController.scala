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
import models.{Mode, PaymentPlanType}
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DateSetupQuery, DirectDebitReferenceQuery, PaymentReferenceQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import java.time.{LocalDate, LocalDateTime}
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
    implicit request => {
      val userAnswers = request.userAnswers

      val (rows, backLink) = userAnswers.get(AmendPaymentPlanTypePage) match {
        case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
          (Seq(
            AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
            AmendPaymentPlanSourceSummary.row(userAnswers.get(AmendPaymentPlanSourcePage).getOrElse("")),
            DateSetupSummary.row(userAnswers.get(DateSetupQuery).getOrElse(LocalDateTime.now())),
            TotalAmountDueSummary.row(userAnswers.get(TotalAmountDuePage).getOrElse(BigDecimal(0))),
            MonthlyPaymentAmountSummary.row(userAnswers.get(MonthlyPaymentAmountPage).getOrElse(0), userAnswers.get(TotalAmountDuePage).getOrElse(0)),
            FinalPaymentAmountSummary.row(userAnswers.get(FinalPaymentAmountPage).getOrElse(0), userAnswers.get(TotalAmountDuePage).getOrElse(0)),
            PaymentsFrequencySummary.row2(userAnswers.get(PaymentsFrequencyPage).get.toString),
            AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, userAnswers.get(AmendPlanStartDatePage).getOrElse(LocalDate.now())),
            AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, userAnswers.get(AmendPaymentAmountPage).getOrElse(BigDecimal(0))),
            AmendPlanEndDateSummary.row(userAnswers.get(AmendPlanEndDatePage).getOrElse(LocalDate.now())),
          ), routes.AmendPlanEndDateController.onPageLoad(mode))
        case _ =>
          (Seq(
            AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
            AmendPaymentPlanSourceSummary.row(userAnswers.get(AmendPaymentPlanSourcePage).getOrElse("")),
            DateSetupSummary.row(userAnswers.get(DateSetupQuery).getOrElse(LocalDateTime.now())),
            AmendPlanEndDateSummary.row(userAnswers.get(AmendPlanEndDatePage).getOrElse(LocalDate.now())),
            AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, userAnswers.get(AmendPaymentAmountPage).getOrElse(BigDecimal(0))),
            AmendPlanStartDateSummary.row(PaymentPlanType.SinglePaymentPlan.toString, userAnswers.get(AmendPlanStartDatePage).getOrElse(LocalDate.now())),
          ), routes.AmendPlanStartDateController.onPageLoad(mode))
      }
      for {
        bankDetailsWithAuddisStatus <- Future.fromTry(Try(userAnswers.get(YourBankDetailsPage).get))
        directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
        paymentReference <- Future.fromTry(Try(userAnswers.get(PaymentReferenceQuery).get))
        planType <- Future.fromTry(Try(userAnswers.get(AmendPaymentPlanTypePage).get))
      } yield {
        Ok(view(mode, paymentReference, directDebitReference, bankDetailsWithAuddisStatus.sortCode,
          bankDetailsWithAuddisStatus.accountNumber, rows, backLink))
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
      Future.successful(Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()))
  }

}
