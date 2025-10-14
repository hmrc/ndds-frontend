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
import forms.CancelPaymentPlanFormProvider
import models.{NormalMode, PaymentPlanType}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.CancelPaymentPlanPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import views.html.CancelPaymentPlanView

import scala.concurrent.Future

class CancelPaymentPlanControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new CancelPaymentPlanFormProvider()
  private val form = formProvider()

  private lazy val cancelPaymentPlanRoute = routes.CancelPaymentPlanController.onPageLoad(NormalMode).url

  "CancelPaymentPlan Controller" - {

    "must return OK and the correct view for a GET with BudgetPaymentPlan" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val paymentPlanReference = "ppReference"

      val paymentPlan = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, cancelPaymentPlanRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelPaymentPlanView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get)(
            request,
            messages(application)
          ).toString
      }
    }

    "must return OK and the correct view for a GET with SinglePaymentPlan" in {

      val mockSinglePaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
        )

      val paymentPlanReference = "ppReference"

      val paymentPlan = mockSinglePaymentPlanDetailResponse.paymentPlanDetails

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockSinglePaymentPlanDetailResponse
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, cancelPaymentPlanRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelPaymentPlanView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get)(
            request,
            messages(application)
          ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val paymentPlanReference = "ppReference"

      val paymentPlan = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value
          .set(CancelPaymentPlanPage, true)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, cancelPaymentPlanRoute)

        val view = application.injector.instanceOf[CancelPaymentPlanView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(true), NormalMode, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get)(
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, cancelPaymentPlanRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val paymentPlanReference = "ppReference"

      val paymentPlan = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request =
          FakeRequest(POST, cancelPaymentPlanRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CancelPaymentPlanView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm,
                                               NormalMode,
                                               paymentPlan.planType,
                                               paymentPlanReference,
                                               paymentPlan.scheduledPaymentAmount.get
                                              )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, cancelPaymentPlanRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if empty data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, cancelPaymentPlanRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, cancelPaymentPlanRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if empty data is found when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, cancelPaymentPlanRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
