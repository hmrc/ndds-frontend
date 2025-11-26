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

package controllers.testonly

import base.SpecBase
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import forms.RegularPaymentAmountFormProvider
import models.{CheckMode, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.RegularPaymentAmountPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.testonly.TestOnlyAmendRegularPaymentAmountView

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class TestOnlyAmendRegularPaymentAmountControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new RegularPaymentAmountFormProvider()
  private val form = formProvider()
  private val route = "/test-only/regular-payment-amount"
  private val validAnswer = BigDecimal(150.73).setScale(2, RoundingMode.HALF_UP)

  private def buildApplication(mockService: NationalDirectDebitService) =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[NationalDirectDebitService].toInstance(mockService))
      .build()

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"TestOnlyAmendRegularPaymentAmountController in $mode" - {

      "must return OK and the correct view for a GET" in {
        val mockService = mock[NationalDirectDebitService]
        val application = buildApplication(mockService)

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(GET, route)
          val result = controller.onPageLoad(mode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            mode,
            testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )(request, messages(application)).toString
        }
      }

      "must populate the view on a GET when the question has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .set(RegularPaymentAmountPage, validAnswer)
          .success
          .value

        val mockService = mock[NationalDirectDebitService]
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(GET, route)
          val result = controller.onPageLoad(mode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(validAnswer),
            mode,
            testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )(request, messages(application)).toString
        }
      }

      "must redirect to journey recovery if amend payment plan guard returns false" in {
        val mockService = mock[NationalDirectDebitService]
        val application = buildApplication(mockService)

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(GET, route)
          val result = controller.onPageLoad(mode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(GET, route)
          val result = controller.onPageLoad(mode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the confirmation page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]
        val mockService = mock[NationalDirectDebitService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockService)
          )
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(POST, route).withFormUrlEncodedBody(("value", validAnswer.toString))
          val result = controller.onSubmit(mode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad().url
        }
      }

      "must return a Bad Request when invalid data is submitted" in {
        val mockService = mock[NationalDirectDebitService]
        val application = buildApplication(mockService)

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(POST, route).withFormUrlEncodedBody(("value", "invalid value"))
          val boundForm = form.bind(Map("value" -> "invalid value"))
          val view = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountView]
          val result = controller.onSubmit(mode)(request)

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            mode,
            testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendRegularPaymentAmountController]
          val request = FakeRequest(POST, route).withFormUrlEncodedBody(("value", validAnswer.toString))
          val result = controller.onSubmit(mode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
