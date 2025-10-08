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
import forms.PaymentAmountFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.PaymentAmountPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PaymentAmountView
import models.DirectDebitSource.PAYE
import pages.DirectDebitSourcePage
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class PaymentAmountControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new PaymentAmountFormProvider()
  private val form = formProvider()

  private def onwardRoute = Call("GET", "/foo")

  private val validAnswer = BigDecimal(1.00).setScale(2, RoundingMode.HALF_UP)

  private lazy val paymentAmountRoute = routes.PaymentAmountController.onPageLoad(NormalMode).url

  "PaymentAmount Controller" - {

    lazy val paymentReferenceRoute = routes.PaymentReferenceController.onPageLoad(NormalMode).url
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentAmountRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Call("GET", paymentReferenceRoute))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(PaymentAmountPage, validAnswer).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentAmountRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, Call("GET", paymentReferenceRoute))(request,
                                                                                                                       messages(application)
                                                                                                                      ).toString
      }
    }

    "must return OK and the correct view for a GET when DirectDebitSourcePage is PAYE" in {
      val userAnswers = emptyUserAnswers.set(DirectDebitSourcePage, PAYE).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentAmountRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, routes.YearEndAndMonthController.onPageLoad(NormalMode))(request,
                                                                                                                          messages(application)
                                                                                                                         ).toString
      }
    }

    "must return Bad Request and errors when invalid data is submitted and DirectDebitSourcePage is PAYE" in {
      val userAnswers = emptyUserAnswers.set(DirectDebitSourcePage, PAYE).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, paymentAmountRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PaymentAmountView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, routes.YearEndAndMonthController.onPageLoad(NormalMode))(request,
                                                                                                                               messages(application)
                                                                                                                              ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and DirectDebitSourcePage is PAYE" in {
      val userAnswers = emptyUserAnswers.set(DirectDebitSourcePage, PAYE).success.value

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, paymentAmountRoute)
          .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, paymentAmountRoute)
          .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, paymentAmountRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PaymentAmountView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", paymentReferenceRoute))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, paymentAmountRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, paymentAmountRoute)
          .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
