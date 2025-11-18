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
import forms.DirectDebitSourceFormProvider
import models.{DirectDebitSource, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.DirectDebitSourcePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AddPaymentPlanIdentifierQuery
import repositories.SessionRepository
import views.html.DirectDebitSourceView

import scala.concurrent.Future

class DirectDebitSourceControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val directDebitSourceRoute = routes.DirectDebitSourceController.onPageLoad(NormalMode).url
  lazy val ConfirmAuthorityRoute = routes.ConfirmAuthorityController.onPageLoad(NormalMode).url
  lazy val bankDetailsAnswerRoute = routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode).url
  lazy val directDebitRoute = routes.DirectDebitSummaryController.onPageLoad().url

  val formProvider = new DirectDebitSourceFormProvider()
  val form = formProvider()

  "DirectDebitSource Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, directDebitSourceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DirectDebitSourceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Call("GET", ConfirmAuthorityRoute))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(DirectDebitSourcePage, DirectDebitSource.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, directDebitSourceRoute)

        val view = application.injector.instanceOf[DirectDebitSourceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(DirectDebitSource.values.head), NormalMode, Call("GET", ConfirmAuthorityRoute))(
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
          FakeRequest(POST, directDebitSourceRoute)
            .withFormUrlEncodedBody(("value", DirectDebitSource.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, directDebitSourceRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[DirectDebitSourceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", ConfirmAuthorityRoute))(request, messages(application)).toString
      }
    }

    "redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, directDebitSourceRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, directDebitSourceRoute)
            .withFormUrlEncodedBody(("value", DirectDebitSource.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must start setting up a new payment plan from the Direct Debit page" - {

      "must return OK and the correct view for a GET" in {

        val userAnswersWithDirectDebitReference =
          emptyUserAnswers
            .set(
              AddPaymentPlanIdentifierQuery,
              "directDebitReference"
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithDirectDebitReference)).build()

        running(application) {
          val request = FakeRequest(GET, directDebitSourceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DirectDebitSourceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, Call("GET", directDebitRoute))(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val userAnswersWithDirectDebitReference =
          emptyUserAnswers
            .set(
              AddPaymentPlanIdentifierQuery,
              "directDebitReference"
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithDirectDebitReference)).build()

        running(application) {
          val request =
            FakeRequest(POST, directDebitSourceRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[DirectDebitSourceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", directDebitRoute))(request, messages(application)).toString
        }
      }
    }
  }
}
