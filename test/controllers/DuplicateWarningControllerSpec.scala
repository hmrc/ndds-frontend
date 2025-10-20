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
import forms.DuplicateWarningFormProvider
import models.{NormalMode, PaymentPlanType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.ManagePaymentPlanTypePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.DuplicateWarningView

import scala.concurrent.Future

class DuplicateWarningControllerSpec extends SpecBase {

  private val formProvider = new DuplicateWarningFormProvider()
  private val form = formProvider()
  private val mode = NormalMode

  "DuplicateWarningController" - {

    "must return OK and view with AmendPlanStartDateController back link when planType is SinglePaymentPlan" in {
      val userAnswers = emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.DuplicateWarningController.onPageLoad(mode).url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[DuplicateWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          mode,
          routes.AmendPlanStartDateController.onPageLoad(mode)
        )(request, messages(application)).toString
      }
    }

    "must return OK and view with AmendPlanEndDateController back link when planType is BudgetPaymentPlan" in {
      val userAnswers = emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.DuplicateWarningController.onPageLoad(mode).url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[DuplicateWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          mode,
          routes.AmendPlanEndDateController.onPageLoad(mode)
        )(request, messages(application)).toString
      }
    }

    "must redirect to AmendPaymentPlanConfirmationController when user selects Yes (true)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.DuplicateWarningController.onSubmit(mode).url)
          .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AmendPaymentPlanConfirmationController.onPageLoad(mode).url
      }
    }

    "must redirect to PaymentPlanDetailsController when user selects No (false)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.DuplicateWarningController.onSubmit(mode).url)
          .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentPlanDetailsController.onPageLoad().url
      }
    }
  }
}
