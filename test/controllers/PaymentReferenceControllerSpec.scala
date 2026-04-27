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

  def onwardRoute: Call = Call("GET", "/direct-debits/year-end-and-month")

  private val formProvider = new PaymentReferenceFormProvider()
  private def formFor(source: Option[DirectDebitSource]): Form[String] =
    formProvider(source, None)

  lazy val directDebitSourceRoute = routes.DirectDebitSourceController.onPageLoad(NormalMode)
  lazy val paymentReferenceRoute = routes.PaymentReferenceController.onPageLoad(NormalMode)
  lazy val paymentPlanTypeRoute = routes.PaymentPlanTypeController.onPageLoad(NormalMode)

  "paymentReference Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, MGD)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

      running(application) {
        val request = FakeRequest(GET, paymentReferenceRoute.url)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(MGD))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form,
          NormalMode,
          Some(MGD),
          Call("GET", paymentPlanTypeRoute.url)
        )(request, messages(application)).toString

        contentAsString(result) must include(
          "You can find this on your MGD Registration Certificate. It’s 14 characters long and begins with X, then an " +
            "uppercase letter, then ‘M0000’ and then 7 numbers, such as XVM00005554321."
        )
      }
    }

    Seq(
      (DirectDebitSource.CT, () => directDebitSourceRoute),
      (DirectDebitSource.NIC, () => directDebitSourceRoute),
      (DirectDebitSource.OL, () => directDebitSourceRoute),
      (DirectDebitSource.PAYE, () => directDebitSourceRoute),
      (DirectDebitSource.SA, () => directDebitSourceRoute),
      (DirectDebitSource.SDLT, () => directDebitSourceRoute),
      (DirectDebitSource.TC, () => paymentPlanTypeRoute),
      (DirectDebitSource.VAT, () => directDebitSourceRoute)
    ).foreach { (directDebitSource, expectedBack) =>
      s"must render the view with DirectDebitSourceController route on GET for source $directDebitSource" in {
        val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, directDebitSource)
        val application = applicationBuilder(userAnswers = Some(userAnswer)).build()

        running(application) {
          val request = FakeRequest(GET, paymentReferenceRoute.url)
          val result = route(application, request).value

          val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
          val form = formFor(Some(directDebitSource))

          status(result) mustEqual OK
          contentAsString(result) mustEqual paymentReferenceView(
            form,
            NormalMode,
            Some(directDebitSource),
            expectedBack()
          )(request, messages(application)).toString
        }
      }
    }

    "show the correct back link for SA is BPP's are enabled for SA" in {
      val userAnswer = emptyUserAnswers.setOrException(DirectDebitSourcePage, DirectDebitSource.SA)
      val application = applicationBuilder(userAnswers = Some(userAnswer), additionalConfig = Map("features.sa-bpp" -> true)).build()

      running(application) {
        val request = FakeRequest(GET, paymentReferenceRoute.url)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(DirectDebitSource.SA))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form,
          NormalMode,
          Some(DirectDebitSource.SA),
          paymentPlanTypeRoute
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
        val request = FakeRequest(GET, paymentReferenceRoute.url)
        val result = route(application, request).value

        val paymentReferenceView = application.injector.instanceOf[PaymentReferenceView]
        val form = formFor(Some(TC))

        status(result) mustEqual OK
        contentAsString(result) mustEqual paymentReferenceView(
          form.fill("answer"),
          NormalMode,
          Some(TC),
          Call("GET", paymentPlanTypeRoute.url)
        )(request, messages(application)).toString

        contentAsString(result) must include(
          "You can find this on letters from HMRC"
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
          FakeRequest(POST, paymentReferenceRoute.url)
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
          FakeRequest(POST, paymentReferenceRoute.url)
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
          FakeRequest(POST, paymentReferenceRoute.url)
            .withFormUrlEncodedBody(("value", "WT447571311207NE"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentAmountController.onPageLoad(NormalMode).url
      }
    }
  }
}
