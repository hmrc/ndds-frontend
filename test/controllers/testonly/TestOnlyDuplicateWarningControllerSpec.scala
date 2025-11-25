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

import controllers.testonly.routes as testOnlyRoutes
import base.SpecBase
import forms.DuplicateWarningFormProvider
import models.{NormalMode, PaymentPlanType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{DuplicateWarningPage, ManagePaymentPlanTypePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.testonly.TestOnlyDuplicateWarningView

import scala.concurrent.Future

class TestOnlyDuplicateWarningControllerSpec extends SpecBase {

  private val formProvider = new DuplicateWarningFormProvider()
  private val form = formProvider()
  private val mode = NormalMode

  "TestOnlyDuplicateWarningController" - {
    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "must return OK and view TestOnlyDuplicateWarningController onPageLoad" in {
      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(ua)))

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(GET, routes.TestOnlyDuplicateWarningController.onPageLoad(mode).url)
        val result = controller.onPageLoad(NormalMode)(request)
        val view = application.injector.instanceOf[TestOnlyDuplicateWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          mode,
          testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to AmendPaymentPlanUpdateController when user selects Yes (true)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(POST, routes.TestOnlyDuplicateWarningController.onPageLoad(mode).url).withFormUrlEncodedBody(("value", "true"))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url
      }
    }

    "must redirect to AmendPaymentPlanConfirmationController when user selects No (false)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(POST, routes.TestOnlyDuplicateWarningController.onPageLoad(mode).url).withFormUrlEncodedBody(("value", "false"))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad().url
      }
    }

    "must redirect to page not found if already value is submitted and click browser back from Updated page" in {
      val ua =
        emptyUserAnswers
          .set(DuplicateWarningPage, true)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(ua)))

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(GET, routes.TestOnlyDuplicateWarningController.onPageLoad(mode).url)
        val result = controller.onPageLoad(NormalMode)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.BackSubmissionController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery page when amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(GET, routes.TestOnlyDuplicateWarningController.onPageLoad(mode).url)
        val result = controller.onPageLoad(mode)(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
