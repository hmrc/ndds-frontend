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
import views.html.SetupDirectDebitPaymentView

class SetupDirectDebitPaymentControllerSpec extends SpecBase {

  "SetupDirectDebitPayment Controller" - {

    "must return OK and the correct view for a GET with no back link (DDI = 0) without Back link" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad(0).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SetupDirectDebitPaymentView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(0)(request, messages(application)).toString
        contentAsString(result) must not include ("Back")
      }
    }

      "must return OK and the correct view for a GET if there is back link (DDI > 1) with Back link" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad(5).url)

          val result = route(application, request).value

          status(result) mustEqual OK

          contentAsString(result) must include ("Back")

          contentAsString(result) must include ("Setup a direct debit payment")
          contentAsString(result) must include ("Please note")

          contentAsString(result) must include ("Start now")
        }

      }
  }
}
