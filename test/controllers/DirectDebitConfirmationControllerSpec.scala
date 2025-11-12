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
import models.PaymentDateDetails
import models.responses.GenerateDdiRefResponse
import pages.{CheckYourAnswerPage, PaymentDatePage}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.govuk.all.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import views.html.DirectDebitConfirmationView

import java.time.LocalDate

class DirectDebitConfirmationControllerSpec extends SpecBase {

  "DirectDebitConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {
      val ddiRefNumber = "ddiRef"
      val ppRef = "ppRef"

      val userAnswers = emptyUserAnswers
        .setOrException(CheckYourAnswerPage, GenerateDdiRefResponse(ddiRefNumber))
        .setOrException(PaymentDatePage, PaymentDateDetails(LocalDate.of(2025, 12, 12), "earliest"))

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
            SummaryListRowViewModel(
              key     = Key(Text("Direct Debit reference")),
              value   = ValueViewModel(Text(ddiRefNumber)),
              actions = Seq.empty
            )
          )
        )

        val paymentDateString = "12 December 2025"
        val paymentPlanDetails = SummaryListViewModel(
          rows = Seq(
            SummaryListRowViewModel(
              key     = Key(Text("Payment reference")),
              value   = ValueViewModel(Text("Missing payment reference")),
              actions = Seq.empty
            ),
            SummaryListRowViewModel(
              key     = Key(Text("Payment date")),
              value   = ValueViewModel(Text(paymentDateString)),
              actions = Seq.empty
            )
          )
        )

        val expectedHtml = view(
          appConfig.hmrcHelplineUrl,
          ddiRefNumber,
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
