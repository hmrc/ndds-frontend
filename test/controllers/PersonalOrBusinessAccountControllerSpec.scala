/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.PersonalOrBusinessAccountFormProvider
import models.{NormalMode, PersonalOrBusinessAccount, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.PersonalOrBusinessAccountPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, *}
import repositories.SessionRepository
import views.html.PersonalOrBusinessAccountView

import scala.concurrent.Future

class PersonalOrBusinessAccountControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val personalOrBusinessAccountRoute: String = routes.PersonalOrBusinessAccountController.onPageLoad(NormalMode).url

  val formProvider = new PersonalOrBusinessAccountFormProvider()
  val form: Form[PersonalOrBusinessAccount] = formProvider()

  lazy val backLinkRoute: Call = routes.SetupDirectDebitPaymentController.onPageLoad()

  "PersonalOrBusinessAccount Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, personalOrBusinessAccountRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PersonalOrBusinessAccountView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, personalOrBusinessAccountRoute)

        val view = application.injector.instanceOf[PersonalOrBusinessAccountView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(PersonalOrBusinessAccount.values.head), NormalMode, backLinkRoute)(request,
                                                                                                                            messages(application)
                                                                                                                           ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and no previous data" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, personalOrBusinessAccountRoute)
            .withFormUrlEncodedBody(("value", PersonalOrBusinessAccount.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
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
          FakeRequest(POST, personalOrBusinessAccountRoute)
            .withFormUrlEncodedBody(("value", PersonalOrBusinessAccount.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, personalOrBusinessAccountRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PersonalOrBusinessAccountView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, backLinkRoute)(request, messages(application)).toString
        contentAsString(result) must include("Invalid value")
      }
    }

    "must return a Bad Request and errors when no option is selected" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, personalOrBusinessAccountRoute)
            .withFormUrlEncodedBody()

        val boundForm = form.bind(Map.empty)

        val view = application.injector.instanceOf[PersonalOrBusinessAccountView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }

    "must display the page correctly for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, personalOrBusinessAccountRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }
}
