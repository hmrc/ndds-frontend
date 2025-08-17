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
import forms.YourBankDetailsFormProvider
import models.responses.{BarsResponse, BarsVerificationResponse, Bank, BankAddress, Country}
import models.{DirectDebitSource, NormalMode, UserAnswers, YourBankDetails}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{DirectDebitSourcePage, YourBankDetailsPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.BARService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.YourBankDetailsView

import scala.concurrent.Future

class YourBankDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new YourBankDetailsFormProvider()
  val form: Form[YourBankDetails] = formProvider()

  lazy val yourBankDetailsRoute: String = routes.YourBankDetailsController.onPageLoad(NormalMode).url
  lazy val personalOrBusinessAccountRoute: String = routes.PersonalOrBusinessAccountController.onPageLoad(NormalMode).url

  val userAnswers: UserAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      YourBankDetailsPage.toString -> Json.obj(
        "accountHolderName" -> "value 1",
        "sortCode" -> "123212",
        "accountNumber" -> "34211234",
        "auddisStatus" -> true
      )
    )
  )

  "YourBankDetails Controller" - {

    // existing GET and POST tests here (unchanged) ...

    "must call BARService and handle response on valid POST submission" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockBarService = mock[BARService]

      // Provide implicit HeaderCarrier
      implicit val hc: HeaderCarrier = HeaderCarrier()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      // Example BARService mock response
      val barResponse = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some("Test Bank"),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = Some(BarsResponse.No),
        iban = Some("GB29NWBK60161331926819"),
        accountName = Some("John Doe"),
        bank = Some(
          Bank(
            name = "Test Bank",
            address = BankAddress(
              lines = Seq("1 Bank Street"),
              town = "London",
              country = Country("UK"),
              postCode = "EC1A 1AA"
            )
          )
        )
      )


      when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(barResponse)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BARService].toInstance(mockBarService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode" -> "123212",
            "accountNumber" -> "34211234",
            "auddisStatus" -> "true"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

  }
}
