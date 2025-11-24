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
import models.PaymentPlanType
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import config.CurrencyFormatter.currencyFormat
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import views.html.DirectDebitConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.math.BigDecimal.RoundingMode

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

    val paymentAmount =
      request.userAnswers
        .get(PaymentAmountPage)
        .orElse(request.userAnswers.get(TotalAmountDuePage).map { totalAmount =>
          (totalAmount / 12).setScale(2, RoundingMode.DOWN)
        })
        .orElse(request.userAnswers.get(RegularPaymentAmountPage))
        .getOrElse(BigDecimal(0))

    val formattedPaymentAmount = currencyFormat(paymentAmount)

    val paymentDate: LocalDate =
      request.userAnswers
        .get(PlanStartDatePage)
        .map(_.enteredDate)
        .orElse(
          request.userAnswers.get(PaymentDatePage).map(_.enteredDate)
        )
        .getOrElse(throw new Exception("Missing date"))

    val paymentDateString: String = paymentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    val monthlyPaymentAmount = if (request.userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
      MonthlyPaymentAmountSummary.row(request.userAnswers)
    } else {
      None
    }
    val finalPaymentAmount = if (request.userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
      FinalPaymentAmountSummary.row(request.userAnswers)
    } else {
      None
    }
    val finalPaymentDate = if (request.userAnswers.get(PaymentPlanTypePage).contains(PaymentPlanType.TaxCreditRepaymentPlan)) {
      FinalPaymentDateSummary.row(request.userAnswers, appConfig)
    } else {
      None
    }

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
        YourBankDetailsAccountNumberSummary.row(request.userAnswers, false),
        YourBankDetailsSortCodeSummary.row(request.userAnswers, false)
      ).flatten
    )

    val dateSetupRow: Option[SummaryListRow] = Some(
      SummaryListRowViewModel(
        key = Key(Text("Date set up")),
        value = ValueViewModel(
          Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))
          )
        ),
        actions = Seq.empty
      )
    )

    val list = SummaryListViewModel(
      rows = Seq(
        DirectDebitSourceSummary.rowNoAction(request.userAnswers),
        PaymentPlanTypeSummary.rowNoAction(request.userAnswers),
        PaymentReferenceSummary.rowNoAction(request.userAnswers),
        dateSetupRow,
        PaymentsFrequencySummary.rowData(request.userAnswers),
        RegularPaymentAmountSummary.rowData(request.userAnswers),
        PaymentAmountSummary.row(request.userAnswers, false),
        PaymentDateSummary.row(request.userAnswers, false),
        TotalAmountDueSummary.rowData(request.userAnswers),
        PlanStartDateSummary.row(request.userAnswers, false),
        PlanEndDateSummary.rowData(request.userAnswers),
        monthlyPaymentAmount,
        finalPaymentDate,
        finalPaymentAmount
      ).flatten
    )

    Ok(
      view(
        appConfig.hmrcHelplineUrl,
        referenceNumber.ddiRefNumber,
        formattedPaymentAmount,
        paymentDateString,
        directDebitDetails,
        list
      )
    )
  }

}
