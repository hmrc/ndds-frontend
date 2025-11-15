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
import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import pages.{CheckYourAnswerPage, DirectDebitSourcePage, PaymentDatePage, PaymentPlanTypePage, PaymentReferencePage, PlanStartDatePage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{DirectDebitSourceSummary, FinalPaymentAmountSummary, MonthlyPaymentAmountSummary, PaymentAmountSummary, PaymentDateSummary, PaymentPlanTypeSummary, PaymentReferenceSummary, PaymentsFrequencySummary, PlanEndDateSummary, PlanStartDateSummary, RegularPaymentAmountSummary, TotalAmountDueSummary, YearEndAndMonthSummary, YourBankDetailsAccountHolderNameSummary, YourBankDetailsAccountNumberSummary, YourBankDetailsSortCodeSummary}
import viewmodels.govuk.all.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import views.html.DirectDebitConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DirectDebitConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: DirectDebitConfirmationView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val referenceNumber = request.userAnswers
      .get(CheckYourAnswerPage)
      .getOrElse(throw new Exception("Missing generated DDI reference number"))

    val paymentDate: LocalDate = request.userAnswers
      .get(PlanStartDatePage)
      .map(_.enteredDate)
      .getOrElse(throw new Exception("Missing entered payment date"))

    val paymentDateString: String = paymentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    val directDebitDetails = SummaryListViewModel(
      rows = Seq(
        Option(
          SummaryListRowViewModel(
            key     = Key(Text("Direct Debit reference")),
            value   = ValueViewModel(Text(referenceNumber.ddiRefNumber)),
            actions = Seq.empty
          )
        ),
        YourBankDetailsAccountHolderNameSummary.row(request.userAnswers, false),
        YourBankDetailsSortCodeSummary.row(request.userAnswers, false),
        YourBankDetailsAccountNumberSummary.row(request.userAnswers, false)
      ).flatten
    )
    val summaryRows: SummaryList = buildSummaryRows(request.userAnswers)

    Ok(
      view(
        appConfig.hmrcHelplineUrl,
        referenceNumber.ddiRefNumber,
        paymentDateString,
        directDebitDetails,
        summaryRows
      )
    )
  }

  private def buildSummaryRows(userAnswers: UserAnswers)(implicit messages: Messages): SummaryList = {

    val planType = userAnswers.get(PaymentPlanTypePage).get

    val firstRow: Seq[SummaryListRow] = Seq(
      SummaryListRowViewModel(
        key = Key(Text("Payment reference")),
        value = ValueViewModel(
          Text(
            userAnswers
              .get(PaymentReferencePage)
              .getOrElse("Missing payment reference")
          )
        ),
        actions = Seq.empty
      )
    )

    val dateSetupRow: Option[SummaryListRow] = Some(
      SummaryListRowViewModel(
        key = Key(Text("Date set up")),
        value = ValueViewModel(
          Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          )
        ),
        actions = Seq.empty
      )
    )

    val directDebitSource = userAnswers.get(DirectDebitSourcePage)
    val showStartDate: Option[SummaryListRow] =
      if (directDebitSource.contains(DirectDebitSource.PAYE)) {
        YearEndAndMonthSummary.row(userAnswers)
      } else {
        PlanStartDateSummary.row(userAnswers, false)
      }

    val showEndDate: Option[SummaryListRow] =
      if (directDebitSource.contains(DirectDebitSource.SA)) {
        if (planType == PaymentPlanType.BudgetPaymentPlan)
          PlanEndDateSummary.row(userAnswers, false)
        else
          None
      } else if (directDebitSource.contains(DirectDebitSource.TC)) {
        PlanEndDateSummary.row(userAnswers, false)
      } else {
        None
      }

    val planRows: Seq[SummaryListRow] = planType match {

      case PaymentPlanType.SinglePaymentPlan =>
        Seq(
          PaymentPlanTypeSummary.row(userAnswers, false),
          DirectDebitSourceSummary.row(userAnswers)
        ).flatten ++
          dateSetupRow.toSeq ++
          Seq(
            PaymentAmountSummary.row(userAnswers),
            PaymentDateSummary.row(userAnswers)
          ).flatten

      case PaymentPlanType.BudgetPaymentPlan =>
        Seq(
          PaymentPlanTypeSummary.row(userAnswers, false),
          DirectDebitSourceSummary.row(userAnswers)
        ).flatten ++
          dateSetupRow.toSeq ++
          Seq(
            RegularPaymentAmountSummary.row(userAnswers),
            PaymentsFrequencySummary.row(userAnswers),
            showStartDate,
            showEndDate
          ).flatten

      case PaymentPlanType.TaxCreditRepaymentPlan =>
        Seq(
          PaymentPlanTypeSummary.row(userAnswers, false),
          DirectDebitSourceSummary.row(userAnswers)
        ).flatten ++
          dateSetupRow.toSeq ++
          Seq(
            TotalAmountDueSummary.row(userAnswers, false),
            MonthlyPaymentAmountSummary.row(userAnswers),
            FinalPaymentAmountSummary.row(userAnswers),
            showStartDate,
            showEndDate
          ).flatten

      case PaymentPlanType.VariablePaymentPlan =>
        Seq(
          PaymentPlanTypeSummary.row(userAnswers, false),
          DirectDebitSourceSummary.row(userAnswers)
        ).flatten ++
          dateSetupRow.toSeq ++
          Seq(
            showStartDate
          ).flatten

      case _ =>
        Seq.empty
    }

    SummaryList(rows = firstRow ++ planRows)
  }

}
