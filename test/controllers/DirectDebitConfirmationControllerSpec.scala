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

import base.SpecBase
import config.CurrencyFormatter.currencyFormat
import models.responses.GenerateDdiRefResponse
import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanType, YourBankDetailsWithAuddisStatus}
import pages.*
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import views.html.DirectDebitConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DirectDebitConfirmationControllerSpec extends SpecBase {

  "DirectDebitConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {
      val ddiRefNumber = "ddiRef"
      val ppRef = "ppRef"
      val paymentAmount = BigDecimal(120)
      val paymentDate = LocalDate.of(2025, 12, 12)
      val bankAccountHolderName = "John Doe"
      val bankAccountNumber = "12345678"
      val bankSortCode = "205142"
      val auddisStatus = true
      val accountVerified = true
      val dateSetup = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))

      val yourBankDetails = YourBankDetailsWithAuddisStatus(
        bankAccountHolderName,
        bankSortCode,
        bankAccountNumber,
        auddisStatus,
        accountVerified
      )

      val userAnswers = emptyUserAnswers
        .setOrException(CheckYourAnswerPage, GenerateDdiRefResponse(ddiRefNumber))
        .setOrException(PaymentDatePage, PaymentDateDetails(paymentDate, "earliest"))
        .setOrException(PaymentAmountPage, paymentAmount)
        .setOrException(DirectDebitSourcePage, DirectDebitSource.PAYE)
        .setOrException(YourBankDetailsPage, yourBankDetails)
        .setOrException(PaymentReferencePage, ppRef)
        .setOrException(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.DirectDebitConfirmationController.onPageLoad().url)

        implicit val messages: Messages =
          application.injector.instanceOf[MessagesApi].preferred(request)

        val result = route(application, request).value

        status(result) mustEqual OK

        val view = application.injector.instanceOf[DirectDebitConfirmationView]
        val appConfig = application.injector.instanceOf[config.FrontendAppConfig]

        val directDebitDetails = SummaryListViewModel(
          rows = Seq(
            Option(
              SummaryListRowViewModel(
                key     = Key(Text("Direct Debit reference")),
                value   = ValueViewModel(Text(ddiRefNumber)),
                actions = Seq.empty
              )
            ),
            YourBankDetailsAccountHolderNameSummary.row(userAnswers, false),
            YourBankDetailsAccountNumberSummary.row(userAnswers, false),
            YourBankDetailsSortCodeSummary.row(userAnswers, false)
          ).collect { case Some(row) => row }
        )

        val formattedPaymentAmount = currencyFormat(paymentAmount)
        val paymentDateString = paymentDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        val paymentPlanDetails = SummaryListViewModel(
          rows = Seq(
            DirectDebitSourceSummary.rowNoAction(userAnswers),
            PaymentPlanTypeSummary.rowNoAction(userAnswers),
            PaymentReferenceSummary.rowNoAction(userAnswers).map(Some(_)).getOrElse(None),
            Some(
              SummaryListRowViewModel(
                key     = Key(Text("Date set up")),
                value   = ValueViewModel(Text(dateSetup)),
                actions = Seq.empty
              )
            ),
            Some(
              SummaryListRowViewModel(
                key     = Key(Text("Payment amount")),
                value   = ValueViewModel(Text(formattedPaymentAmount)),
                actions = Seq.empty
              )
            ),
            PaymentDateSummary.row(userAnswers, false)
          ).flatten
        )
        val expectedHtml = view(
          appConfig.hmrcHelplineUrl,
          ddiRefNumber,
          formattedPaymentAmount,
          paymentDateString,
          directDebitDetails,
          paymentPlanDetails
        )(request, messages).toString

        contentAsString(result) mustEqual expectedHtml
      }
    }

    "must return error if no ddi reference" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.DirectDebitConfirmationController.onPageLoad().url)
        val result = intercept[Exception](route(application, request).value.futureValue)

        result.getMessage must include("Missing generated DDI reference number")

      }
    }

  }
}
