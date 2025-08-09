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
import forms.BankDetailsCheckYourAnswerFormProvider
import models.{CheckMode, NormalMode, YourBankDetailsWithAuddisStatus}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{BankDetailsCheckYourAnswerPage, YourBankDetailsPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AuditService

import scala.concurrent.Future

class BankDetailsCheckYourAnswerControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new BankDetailsCheckYourAnswerFormProvider()
  val form = formProvider()
  val mockAuditService: AuditService = mock[AuditService]

  lazy val bankDetailsCheckYourAnswerRoute = routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode).url

  "BankDetailsCheckYourAnswer Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val html = contentAsString(result)
        html must include("Check your answers")
        html must include("Test Bank Name")
        html must include("Your bank details")
        html must include("Are you the account holder or an authorised signatory, and able to authorise Direct Debit payments from this account?")
        html must include("You are able to authorise Direct Debit payments for the account either as the account holder or on behalf of the multiple signatories.")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "\"")

      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true))
        .set(BankDetailsCheckYourAnswerPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        val html = contentAsString(result)
        html must include("Check your answers")
        html must include("Test Bank Name")
        html must include("Your bank details")
        html must include("Account Holder Name")
        html must include("123212")
        html must include("34211234")
        html must include("Address line 1")
        html must include("Are you the account holder or an authorised signatory, and able to authorise Direct Debit payments from this account?")
        html must include("You are able to authorise Direct Debit payments for the account either as the account holder or on behalf of the multiple signatories.")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "\"")
      }
    }

    "must redirect to the next page when valid data is submitted and send an audit event if yes is selected" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", true))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      doNothing().when(mockAuditService).sendEvent(any())(any(), any(), any())

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsCheckYourAnswerRoute)
            .withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockAuditService).sendEvent(any())(any(), any(), any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", true))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsCheckYourAnswerRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
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

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsCheckYourAnswerRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
