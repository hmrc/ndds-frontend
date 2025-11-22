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
import forms.DuplicateWarningFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.testonly.TestOnlyDuplicateWarningView

import scala.concurrent.Future

class TestOnlyDuplicateWarningControllerSpec extends SpecBase {

  private val formProvider = new DuplicateWarningFormProvider()
  private val form = formProvider()
  private val mode = NormalMode

  "TestOnlyDuplicateWarningController" - {

    "must return OK and view TestOnlyDuplicateWarningController onPageLoad" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(GET, "/test-only/amend-already-have-payment-plan")
        val result = controller.onPageLoad(NormalMode)(request)
        val view = application.injector.instanceOf[TestOnlyDuplicateWarningView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          mode,
          controllers.routes.AmendPaymentPlanConfirmationController.onPageLoad(mode)
        )(request, messages(application)).toString
      }
    }

    "must redirect to AmendPaymentPlanUpdateController when user selects Yes (true)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(POST, "/test-only/amend-already-have-payment-plan").withFormUrlEncodedBody(("value", "true"))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.AmendPaymentPlanUpdateController.onPageLoad().url
      }
    }

    "must redirect to AmendPaymentPlanConfirmationController when user selects No (false)" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val controller = application.injector.instanceOf[TestOnlyDuplicateWarningController]
        val request = FakeRequest(POST, "/test-only/amend-already-have-payment-plan").withFormUrlEncodedBody(("value", "false"))
        val result = controller.onSubmit(NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.AmendPaymentPlanConfirmationController.onPageLoad(mode).url
      }
    }
  }
}
