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
import forms.PaymentDateFormProvider
import models.responses.EarliestPaymentDate
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.{PaymentDateHelper, PaymentDateViewModel}
import views.html.PaymentDateView

import java.time.*
import scala.concurrent.Future

class PaymentDateControllerSpec extends SpecBase with MockitoSugar {

  override def beforeEach(): Unit = {
    reset(mockHelper)
    super.beforeEach()
  }

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new PaymentDateFormProvider()

  private def form = formProvider()

  val mockHelper = mock[PaymentDateHelper]

  def onwardRoute = Call("GET", "/foo")

  val validAnswer = LocalDate.of(2025, 2, 1)

  lazy val paymentDateRoute = routes.PaymentDateController.onPageLoad(NormalMode).url

  override val emptyUserAnswers = UserAnswers(userAnswersId)
  val fixedInstant = Instant.parse("2024-07-17T00:00:00Z")
  val fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault())

  val date = LocalDateTime.now(fixedClock)
  private val formattedDate = "6 February 2025"

  val expectedEarliestPaymentDate = EarliestPaymentDate("2025-02-06")

  val expectedUserAnswersNormalMode = emptyUserAnswers.copy(data =
    Json.obj(
      "yourBankDetails" -> Json.obj(
        "accountHolderName" -> testAccountHolderName,
        "sortCode" -> testSortCode,
        "accountNumber" -> testAccountNumber,
        "auddisStatus" -> true
      )))

  val expectedUserAnswersChangeMode = emptyUserAnswers.copy(data =
    Json.obj(
      "yourBankDetails" -> Json.obj(
        "accountHolderName" -> testAccountHolderName,
        "sortCode" -> testSortCode,
        "accountNumber" -> testAccountNumber,
        "auddisStatus" -> true
      ),
      "paymentDate" -> "2025-02-01"))

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, paymentDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, paymentDateRoute)
      .withFormUrlEncodedBody(
        "value.day" -> "1",
        "value.month" -> "02",
        "value.year" -> "2025"
      )

  "paymentDate Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersNormalMode))
          .overrides(
            bind[Clock].toInstance(fixedClock),
            bind[PaymentDateHelper].toInstance(mockHelper))
          .build()

        when(mockHelper.getEarliestPaymentDate(ArgumentMatchers.eq(expectedUserAnswersNormalMode))(any()))
          .thenReturn(Future.successful(expectedEarliestPaymentDate))

        when(mockHelper.toDateString(ArgumentMatchers.eq(expectedEarliestPaymentDate)))
          .thenReturn(formattedDate)

        running(application) {
          val result = route(application, getRequest()).value

          val view = application.injector.instanceOf[PaymentDateView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, PaymentDateViewModel(NormalMode, formattedDate))(getRequest(), messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersChangeMode))
          .overrides(
            bind[Clock].toInstance(fixedClock),
            bind[PaymentDateHelper].toInstance(mockHelper))
          .build()

        when(mockHelper.getEarliestPaymentDate(ArgumentMatchers.eq(expectedUserAnswersChangeMode))(any()))
          .thenReturn(Future.successful(expectedEarliestPaymentDate))

        when(mockHelper.toDateString(ArgumentMatchers.eq(expectedEarliestPaymentDate)))
          .thenReturn(formattedDate)

        running(application) {
          val view = application.injector.instanceOf[PaymentDateView]

          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), PaymentDateViewModel(NormalMode, formattedDate))(getRequest(), messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val result = route(application, getRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if the earliest payment date cannot be obtained" in {

        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersChangeMode))
          .overrides(
            bind[Clock].toInstance(fixedClock),
            bind[PaymentDateHelper].toInstance(mockHelper))
          .build()

        when(mockHelper.getEarliestPaymentDate(ArgumentMatchers.eq(expectedUserAnswersChangeMode))(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        running(application) {
          val result = route(application, getRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[PaymentDateHelper].toInstance(mockHelper)
            )
            .build()

        running(application) {
          val result = route(application, postRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersNormalMode))
          .overrides(
            bind[Clock].toInstance(fixedClock),
            bind[PaymentDateHelper].toInstance(mockHelper))
          .build()

        when(mockHelper.getEarliestPaymentDate(ArgumentMatchers.eq(expectedUserAnswersNormalMode))(any()))
          .thenReturn(Future.successful(expectedEarliestPaymentDate))

        when(mockHelper.toDateString(ArgumentMatchers.eq(expectedEarliestPaymentDate)))
          .thenReturn(formattedDate)

        val request =
          FakeRequest(POST, paymentDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[PaymentDateView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, PaymentDateViewModel(NormalMode, formattedDate))(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val result = route(application, postRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the earliest payment date cannot be obtained" in {

        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersChangeMode))
          .overrides(
            bind[Clock].toInstance(fixedClock),
            bind[PaymentDateHelper].toInstance(mockHelper))
          .build()

        when(mockHelper.getEarliestPaymentDate(ArgumentMatchers.eq(expectedUserAnswersChangeMode))(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val request =
          FakeRequest(POST, paymentDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
