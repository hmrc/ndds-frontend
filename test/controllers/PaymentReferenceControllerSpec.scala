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
import forms.PaymentReferenceFormProvider
import models.DirectDebitSource.{MGD, TC, VAT}
import models.{DirectDebitSource, NormalMode, PaymentPlanType, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PaymentReferencePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.PaymentReferenceView

import scala.concurrent.Future

class PaymentReferenceControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/directdebits/year-end-and-month")

  private val formProvider = new PaymentReferenceFormProvider()
  private def formFor(source: Option[DirectDebitSource]): Form[String] =
    formProvider(source, None)

  lazy val paymentReferenceRoute: String = routes.PaymentReferenceController.onPageLoad(NormalMode).url
  lazy val paymentPlanTypeRoute: String = routes.PaymentPlanTypeController.onPageLoad(NormalMode).url

  "paymentReference Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, MGD)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      running(application) {
        val request = FakeRequest(GET, paymentReferenceRoute)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(MGD))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form,
          NormalMode,
          Some(MGD),
          Call("GET", paymentPlanTypeRoute)
        )(request, messages(application)).toString

        contentAsString(result) must include(
          "You can find this on the MGD Registration Certificate from HMRC, it begins with an X and is 14 characters long"
        )
      }
    }

    "must render the view with DirectDebitSourceController route on GET for other source" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, DirectDebitSource.OL)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      running(application) {
        val request = FakeRequest(GET, paymentReferenceRoute)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(DirectDebitSource.OL))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form,
          NormalMode,
          Some(DirectDebitSource.OL),
          routes.DirectDebitSourceController.onPageLoad(NormalMode)
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(PaymentReferencePage, "answer")
        .success
        .value
        .setOrException(DirectDebitSourcePage, TC)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentReferenceRoute)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(TC))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form.fill("answer"),
          NormalMode,
          Some(TC),
          Call("GET", paymentPlanTypeRoute)
        )(request, messages(application)).toString

        contentAsString(result) must include(
          "You can find this on letters from HMRC, it is 16 characters long"
        )
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
          FakeRequest(POST, paymentReferenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, VAT)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      lazy val directDebitSourceRoute: String =
        routes.DirectDebitSourceController.onPageLoad(NormalMode).url

      running(application) {
        val request =
          FakeRequest(POST, paymentReferenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(VAT))
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual paymentReferenceView(
          boundForm,
          NormalMode,
          Some(VAT),
          Call("GET", directDebitSourceRoute)
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted even if no existing data is found" in {
      val userAnswers = UserAnswers("id")
        .set(DirectDebitSourcePage, DirectDebitSource.TC)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentReferenceRoute)
            .withFormUrlEncodedBody(("value", "WT447571311207NE"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentAmountController.onPageLoad(NormalMode).url
      }
    }
  }
}
