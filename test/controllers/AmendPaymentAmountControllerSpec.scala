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
import models.{CheckMode, NormalMode, PaymentPlanType}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, ManagePaymentPlanTypePage}
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
  private val onwardRoute = Call("GET", "/direct-debits/check-amendment-details")
  private val validAnswer = BigDecimal(1.00).setScale(2, RoundingMode.HALF_UP)

  "AmendPaymentAmountController" - {

    "must return OK and the correct view for a GET with SinglePaymentPlan" in {
      val userAnswersWithSinglePaymentPlan = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSinglePaymentPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(NormalMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with BudgetPaymentPlan" in {
      val userAnswersWithBudgetPaymentPlan = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPaymentPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(NormalMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(AmendPaymentAmountPage, validAnswer)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(NormalMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          NormalMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to System Error if amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockService = mock[NationalDirectDebitService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", validAnswer.toString))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", "invalid value"))
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view = application.injector.instanceOf[AmendPaymentAmountView]
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to System Error for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", validAnswer.toString))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }

  "AmendPaymentAmountController in CheckMode" - {

    "must return OK and the correct view for a GET with SinglePaymentPlan" in {
      val userAnswersWithSinglePaymentPlan = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSinglePaymentPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(CheckMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          CheckMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with BudgetPaymentPlan" in {
      val userAnswersWithBudgetPaymentPlan = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPaymentPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(CheckMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          CheckMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(AmendPaymentAmountPage, validAnswer)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(CheckMode)(request)
        val view = application.injector.instanceOf[AmendPaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          CheckMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to System Error if amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(CheckMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(GET, "/amount-need-to-pay")
        val result = controller.onPageLoad(CheckMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockService = mock[NationalDirectDebitService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", validAnswer.toString))
        val result = controller.onSubmit(CheckMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", "invalid value"))
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view = application.injector.instanceOf[AmendPaymentAmountView]
        val result = controller.onSubmit(CheckMode)(request)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          CheckMode,
          routes.AmendingPaymentPlanController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to System Error for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val controller = application.injector.instanceOf[AmendPaymentAmountController]
        val request = FakeRequest(POST, "/amount-need-to-pay").withFormUrlEncodedBody(("value", validAnswer.toString))
        val result = controller.onSubmit(CheckMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
