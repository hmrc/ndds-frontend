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
import models.{CheckMode, NormalMode, YourBankDetailsWithAuddisStatus}
import org.scalatestplus.mockito.MockitoSugar
import pages.YourBankDetailsPage
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AuditService

class BankDetailsCheckYourAnswerControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")
  val mockAuditService: AuditService = mock[AuditService]
  lazy val bankDetailsCheckYourAnswerRoute: String = routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode).url

  "BankDetailsCheckYourAnswer Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true, false))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)

        val result = route(application, request).value


        status(result) mustEqual OK

        val html = contentAsString(result)
        html must include("Check your answers")
        html must include("Your bank details")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "\"")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
