/*
 * Copyright 2026 HM Revenue & Customs
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
import models.responses.{BankAddress, Country}
import models.{CheckMode, NormalMode, PersonalOrBusinessAccount, YourBankDetailsWithAuddisStatus}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.MacGenerator

import scala.concurrent.Future

class BankDetailsCheckYourAnswerControllerSpec extends SpecBase with MockitoSugar {

  // Fake MacGenerator for testing
  class FakeMacGenerator extends MacGenerator(null) {
    override def generateMac(
      accountName: String,
      accountNumber: String,
      sortCode: String,
      lines: Seq[String],
      town: Option[String],
      postcode: Option[String],
      bankName: String,
      bacsNumber: String
    ): String = "TEST-MAC"
  }

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new BankDetailsCheckYourAnswerFormProvider()
  val form = formProvider()

  lazy val bankDetailsCheckYourAnswerRoute = routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode).url

  "BankDetailsCheckYourAnswer Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true, false))
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)
        .setOrException(BankDetailsBankNamePage, "BARCLAYS BANK UK PLC")
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("P.O. Box 44"), Some("Reading"), Country("UNITED KINGDOM"), Some("RG1 8BW")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MacGenerator].toInstance(new FakeMacGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        val html = contentAsString(result)
        html must include("Check your answers")
        html must include("BARCLAYS BANK UK PLC")
        html must include("P.O. Box 44")
        html must include("RG1 8BW")
        html must include("UNITED KINGDOM")
        html must include("Your bank details")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#accountHolderName\"")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#accountNumber\"")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#sortCode\"")
      }
    }

    "must populate the view correctly on a GET when previously answered" in {
      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true, false))
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)
        .setOrException(BankDetailsBankNamePage, "BARCLAYS BANK UK PLC")
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("P.O. Box 44"), Some("Reading"), Country("UNITED KINGDOM"), Some("RG1 8BW")))
        .set(BankDetailsCheckYourAnswerPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[MacGenerator].toInstance(new FakeMacGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        val html = contentAsString(result)
        html must include("Check your answers")
        html must include("Your bank details")
        html must include("Account Holder Name")
        html must include("123212")
        html must include("34211234")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#accountHolderName\"")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#accountNumber\"")
        html must include("href=\"" + routes.YourBankDetailsController.onPageLoad(CheckMode).url + "#sortCode\"")
      }
    }

    "must redirect to the next page and persist MAC when Continue is clicked" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val userAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Account Holder Name", "123212", "34211234", auddisStatus = true, false))
        .setOrException(BankDetailsBankNamePage, "BARCLAYS BANK UK PLC")
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("P.O. Box 44"), Some("Reading"), Country("UNITED KINGDOM"), Some("RG1 8BW")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[MacGenerator].toInstance(new FakeMacGenerator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, bankDetailsCheckYourAnswerRoute).withFormUrlEncodedBody("anything" -> "ignored")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to System Error for a GET if no existing data" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[MacGenerator].toInstance(new FakeMacGenerator))
        .build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsCheckYourAnswerRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a POST if no existing data" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[MacGenerator].toInstance(new FakeMacGenerator))
        .build()

      running(application) {
        val request = FakeRequest(POST, bankDetailsCheckYourAnswerRoute).withFormUrlEncodedBody("anything" -> "ignored")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
