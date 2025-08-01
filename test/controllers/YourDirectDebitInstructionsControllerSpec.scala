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
import views.html.YourDirectDebitInstructionsView
import utils.DirectDebitDetailsData

class YourDirectDebitInstructionsControllerSpec extends SpecBase with DirectDebitDetailsData {

  "YourDirectDebitInstructions Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourDirectDebitInstructionsView]
        val directDebits = directDebitDetailsData
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(directDebits)(request, messages(application)).toString
        contentAsString(result) must include("Your direct debit instructions")
        contentAsString(result) must include("You can add a new payment plan to existing Direct Debit Instructions(DDI).")
        contentAsString(result) must include("Direct Debit reference")
        contentAsString(result) must include("Date set up")
        contentAsString(result) must include("Account Number")
        contentAsString(result) must include("Number of payment plans")
        contentAsString(result) must include("View or add to")
      }
    }
  }
}

