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
import config.FrontendAppConfig
import forms.PaymentPlanTypeFormProvider
import models.DirectDebitSource.*
import models.{DirectDebitSource, NormalMode, PaymentPlanType, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{DirectDebitSourcePage, PaymentPlanTypePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PaymentPlanTypeView

import scala.concurrent.Future

class PaymentPlanTypeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val paymentPlanTypeRoute: String =
    routes.PaymentPlanTypeController.onPageLoad(NormalMode).url

  lazy val directDebitSourceRoute: String =
    routes.DirectDebitSourceController.onPageLoad(NormalMode).url

  val formProvider = new PaymentPlanTypeFormProvider()

  private def form(selectedAnswer: Option[DirectDebitSource]): Form[PaymentPlanType] =
    formProvider(selectedAnswer)

  "PaymentPlanType Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, TC)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanTypeRoute)
        val result = route(application, request).value

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[PaymentPlanTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form(Some(TC)),
            NormalMode,
            Some(TC),
            Some(appConfig.selfAssessmentUrl),
            Some(appConfig.paymentProblemUrl),
            Call("GET", directDebitSourceRoute)
          )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered for values1 header" in {

      val userAnswers = UserAnswers(userAnswersId)
        .setOrException(DirectDebitSourcePage, TC)
        .set(PaymentPlanTypePage, PaymentPlanType.values1.head)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanTypeRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(PaymentPlanType.values1.head.toString)
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered for values2 header" in {

      val userAnswers = UserAnswers(userAnswersId)
        .setOrException(DirectDebitSourcePage, TC)
        .set(PaymentPlanTypePage, PaymentPlanType.values2.head)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanTypeRoute)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(PaymentPlanType.values2.head.toString)
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, TC)

      val application =
        applicationBuilder(userAnswers = Some(userAnswer))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, paymentPlanTypeRoute)
            .withFormUrlEncodedBody(("value", PaymentPlanType.values1.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, TC)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentPlanTypeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form(Some(TC)).bind(Map("value" -> "invalid value"))
        val view = application.injector.instanceOf[PaymentPlanTypeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(
            boundForm,
            NormalMode,
            Some(TC),
            None,
            None,
            Call("GET", directDebitSourceRoute)
          )(request, messages(application)).toString
      }
    }

    "must redirect to System Error for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanTypeRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "redirect to System Error for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentPlanTypeRoute)
            .withFormUrlEncodedBody(("value", PaymentPlanType.values1.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
