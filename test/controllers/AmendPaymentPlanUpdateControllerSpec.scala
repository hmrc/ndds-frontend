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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.AmendPaymentPlanUpdateView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

class AmendPaymentPlanUpdateControllerSpec extends SpecBase {

  "PaymentPlanConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {
      //TODO: get payment reference from ap2
      //      val userAnswers = emptyUserAnswers
      //        .setOrException(CheckYourAnswerPage, GenerateDdiRefResponse("600002164"))

      val referenceNumber = "123456789K"
      val paymentAmount: BigDecimal = BigDecimal("1000.00")
      val formattedPaymentAmount: String = NumberFormat.getCurrencyInstance(Locale.UK).format(paymentAmount)
      val startDate: LocalDate = LocalDate.of(2025, 9, 3)
      val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      val endDate: LocalDate = LocalDate.of(2025, 11, 11)
      val formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendPaymentPlanUpdateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(referenceNumber, formattedPaymentAmount, formattedStartDate, formattedEndDate)(request, messages(application)).toString
      }
    }

//    "must return error if no payment reference" in {
//
//      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
//
//      running(application) {
//        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)
//        val result = intercept[Exception](route(application, request).value.futureValue)
//
//        result.getMessage must include("Missing generated Payment reference number")
//
//      }
//    }
  }
}
