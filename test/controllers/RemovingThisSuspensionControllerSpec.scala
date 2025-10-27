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
import forms.RemovingThisSuspensionFormProvider
import models.{NormalMode, UserAnswers}
import models.responses.{PaymentPlanDetails, PaymentPlanResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.RemovingThisSuspensionPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import views.html.RemovingThisSuspensionView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class RemovingThisSuspensionControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemovingThisSuspensionFormProvider()
  val form = formProvider()

  lazy val removingThisSuspensionRoute = routes.RemovingThisSuspensionController.onPageLoad(NormalMode).url

  "RemovingThisSuspension Controller" - {

    "must return OK and the correct view for a GET" in {

      val paymentPlanResponse = dummyPlanDetailResponse.copy(
        paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
          paymentReference    = "1234567890K",
          suspensionStartDate = Some(LocalDate.now().plusDays(5)),
          suspensionEndDate   = Some(LocalDate.now().plusDays(35))
        )
      )

      val userAnswers = emptyUserAnswers.set(PaymentPlanDetailsQuery, paymentPlanResponse).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val paymentPlanResponse = dummyPlanDetailResponse.copy(
        paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
          paymentReference    = "1234567890K",
          suspensionStartDate = Some(LocalDate.now().plusDays(5)),
          suspensionEndDate   = Some(LocalDate.now().plusDays(35))
        )
      )

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(RemovingThisSuspensionPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val paymentPlanResponse = dummyPlanDetailResponse.copy(
        paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
          paymentReference    = "1234567890K",
          suspensionStartDate = Some(LocalDate.now().plusDays(5)),
          suspensionEndDate   = Some(LocalDate.now().plusDays(35))
        )
      )

      val userAnswers = emptyUserAnswers.set(PaymentPlanDetailsQuery, paymentPlanResponse).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val paymentPlanResponse = dummyPlanDetailResponse.copy(
        paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
          paymentReference    = "1234567890K",
          suspensionStartDate = Some(LocalDate.now().plusDays(5)),
          suspensionEndDate   = Some(LocalDate.now().plusDays(35))
        )
      )

      val userAnswers = emptyUserAnswers.set(PaymentPlanDetailsQuery, paymentPlanResponse).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must display suspension details when payment plan has suspension" in {

      val paymentPlanResponse = dummyPlanDetailResponse.copy(
        paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
          paymentReference    = "1234567890K",
          suspensionStartDate = Some(LocalDate.now().plusDays(5)),
          suspensionEndDate   = Some(LocalDate.now().plusDays(35))
        )
      )

      val userAnswers = emptyUserAnswers.set(PaymentPlanDetailsQuery, paymentPlanResponse).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("1234567890K")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
