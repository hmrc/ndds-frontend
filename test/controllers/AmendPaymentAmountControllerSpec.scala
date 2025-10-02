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
import forms.AmendPaymentAmountFormProvider
import models.{NormalMode, PaymentPlanType}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.AmendPaymentPlanTypePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.AmendPaymentAmountView

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class AmendPaymentAmountControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new AmendPaymentAmountFormProvider()
  private val form = formProvider()

  private def onwardRoute = Call("GET", "/foo")
  private val validAnswer = BigDecimal(1.00).setScale(2, RoundingMode.HALF_UP)
  private lazy val paymentPlanAmountRoute = routes.AmendPaymentAmountController.onPageLoad(NormalMode).url

  "PaymentPlanAmount Controller" - {
    lazy val paymentPlanRoute = routes.PaymentPlanDetailsController.onPageLoad().url
    val mockService = mock[NationalDirectDebitService]

    "must return OK and the correct view for a GET with SinglePaymentPlan" in {
      val userAnswersWithSinglePaymentPlan =
        emptyUserAnswers.set(AmendPaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSinglePaymentPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, paymentPlanAmountRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Call("GET", paymentPlanRoute))(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with BudgetPaymentPlan" in {
      val userAnswersWithBudgetPaymentPlan = emptyUserAnswers
        .set(AmendPaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPaymentPlan))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, paymentPlanAmountRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Call("GET", paymentPlanRoute))(request, messages(application)).toString
      }
    }

    "must return NDDS error if amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(AmendPaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)
        val request = FakeRequest(GET, routes.AmendPaymentAmountController.onPageLoad(NormalMode).url)
        val result = intercept[Exception](route(application, request).value.futureValue)

        result.getMessage must include("NDDS Payment Plan Guard: Cannot amend this plan type: taxCreditRepaymentPlan")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanAmountRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, paymentPlanAmountRoute)
          .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, paymentPlanAmountRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[AmendPaymentAmountView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", paymentPlanRoute))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, paymentPlanAmountRoute)
          .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}

