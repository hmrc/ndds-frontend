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
import forms.DuplicateWarningForAddOrCreatePPFormProvider
import models.responses.{BankAddress, Country, GenerateDdiRefResponse}
import models.{DirectDebitSource, NormalMode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetailsWithAuddisStatus}
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
import services.NationalDirectDebitService
import views.html.DuplicateWarningForAddOrCreatePPView

import java.time.LocalDate
import scala.concurrent.Future

class DuplicateWarningForAddOrCreatePPControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new DuplicateWarningForAddOrCreatePPFormProvider()
  val form = formProvider()

  lazy val duplicateWarningForAddOrCreatePPRoute = routes.DuplicateWarningForAddOrCreatePPController.onPageLoad(NormalMode).url

  "DuplicateWarningForAddOrCreatePP Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, duplicateWarningForAddOrCreatePPRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DuplicateWarningForAddOrCreatePPView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(DuplicateWarningForAddOrCreatePPPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, duplicateWarningForAddOrCreatePPRoute)

        val view = application.injector.instanceOf[DuplicateWarningForAddOrCreatePPView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted and answer is no" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, duplicateWarningForAddOrCreatePPRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted and answer is yes" in {

      val fixedDate = LocalDate.of(2025, 7, 19)
      val totalDueAmount = 200
      val planStartDateDetails: PlanStartDateDetails = PlanStartDateDetails(fixedDate, "2025-7-19")

      val incompleteAnswers = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
        .setOrException(TotalAmountDuePage, totalDueAmount)
        .setOrException(PlanStartDatePage, planStartDateDetails)
        .setOrException(PaymentReferencePage, "testReference")
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
        .setOrException(BankDetailsBankNamePage, "Barclays")
        .setOrException(pages.MacValuePage, "valid-mac")

      val mockNddService: NationalDirectDebitService = mock[NationalDirectDebitService]
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.generateNewDdiReference(any())(any()))
        .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))
      when(mockNddService.submitChrisData(any())(any()))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, duplicateWarningForAddOrCreatePPRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DirectDebitConfirmationController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, duplicateWarningForAddOrCreatePPRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DuplicateWarningForAddOrCreatePPView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to System Error for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, duplicateWarningForAddOrCreatePPRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, duplicateWarningForAddOrCreatePPRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
