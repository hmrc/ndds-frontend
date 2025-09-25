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
import forms.AmendPlanStartDateFormProvider
import models.responses.EarliestPaymentDate
import models.{NormalMode, PlanStartDateDetails, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanStartDatePage, PaymentAmountPage, PlanStartDatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.AmendPlanStartDateView
import queries.PaymentPlanTypeQuery

import java.time.*
import scala.concurrent.Future

class AmendPlanStartDateControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  val fixedInstant: Instant = Instant.parse("2024-07-17T00:00:00Z")
  val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneId.systemDefault())
  private val formProvider = new AmendPlanStartDateFormProvider()

  private def form = formProvider()

  val singlePlan = "singlePaymentPlan"

  val mockService: NationalDirectDebitService = mock[NationalDirectDebitService]

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.of(2025, 2, 1)
  val earliestDate: LocalDate = LocalDate.of(2025, 2, 1)
  val amendedValidAnswer: LocalDate = LocalDate.of(2025, 2, 2)

  lazy val amendStartDateRoute: String = routes.AmendPlanStartDateController.onPageLoad(NormalMode).url
  lazy val amendStartDateRoutePost: String = routes.AmendPlanStartDateController.onSubmit(NormalMode).url
  lazy val amendPaymentAmountRoute: String = routes.AmendPaymentAmountController.onPageLoad(NormalMode).url
  
  /** TODO TO be replaced with PP2 route */
  lazy val tempProblemPage: String = routes.JourneyRecoveryController.onPageLoad().url
  
  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val date: LocalDateTime = LocalDateTime.now(fixedClock)

  val expectedEarliestPaymentDate: EarliestPaymentDate = EarliestPaymentDate("2025-02-06")
  val testSortCode = "123456"
  val testAccountNumber = "12345678"
  val testAccountHolderName = "Jon B Jones"

  val expectedUserAnswersNormalMode: UserAnswers = emptyUserAnswers.copy(data =
    Json.obj(
      "yourBankDetails" -> Json.obj(
        "accountHolderName" -> testAccountHolderName,
        "sortCode" -> testSortCode,
        "accountNumber" -> testAccountNumber,
        "auddisStatus" -> true
      )))

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, amendStartDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendStartDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day" -> "1",
        "value.month" -> "02",
        "value.year" -> "2025"
      )

  def postRequestWithDate(date: LocalDate): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendStartDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

  "AmendPlanStartDateController" - {
    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val result = route(application, getRequest()).value
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, Call("GET", amendPaymentAmountRoute))(getRequest(), messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = UserAnswers(userAnswersId).set(AmendPlanStartDatePage, validAnswer).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]
          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, Call("GET", amendPaymentAmountRoute))(getRequest(), messages(application)).toString
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

    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val validDate = LocalDate.parse(expectedEarliestPaymentDate.date)

        val postRequest = FakeRequest(POST, amendStartDateRoutePost)
          .withFormUrlEncodedBody(
            "value.day" -> validDate.getDayOfMonth.toString,
            "value.month" -> validDate.getMonthValue.toString,
            "value.year" -> validDate.getYear.toString
          )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
            )
            .build()

        running(application) {
          val result = route(application, postRequest).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }


      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(expectedUserAnswersNormalMode))
          .build()

        val request =
          FakeRequest(POST, amendStartDateRoutePost)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[AmendPlanStartDateView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual
            view(boundForm, NormalMode, Call("GET", amendPaymentAmountRoute))(request, messages(application)).toString
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

      "must return a Bad Request with amendment.noChange when no amendment is made" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, singlePlan).success.value
          .set(PaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(PlanStartDatePage, PlanStartDateDetails(earliestDate, validAnswer.toString)).success.value
          .set(AmendPlanStartDatePage, validAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          val request = postRequestWithDate(validAnswer)
          val result = route(application, request).value

          val errorForm = form.fill(validAnswer).withError("value", "amendment.noChange")

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual
            view(errorForm, NormalMode, Call("GET", amendPaymentAmountRoute))(request, messages(application)).toString
        }
      }

      "must redirect when amendment in amount is detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, singlePlan).success.value
          .set(PaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPaymentAmountPage, BigDecimal(200.00)).success.value
          .set(PlanStartDatePage, PlanStartDateDetails(earliestDate, validAnswer.toString)).success.value
          .set(AmendPlanStartDatePage, validAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          val request = postRequestWithDate(validAnswer)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
        }
      }

      "must redirect when amendment in date is detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, singlePlan).success.value
          .set(PaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(PlanStartDatePage, PlanStartDateDetails(earliestDate, validAnswer.toString)).success.value
          .set(AmendPlanStartDatePage, amendedValidAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          val request = postRequestWithDate(amendedValidAnswer)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
        }
      }

      "must redirect when amendment in amount and date is detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, singlePlan).success.value
          .set(PaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPaymentAmountPage, BigDecimal(200.00)).success.value
          .set(PlanStartDatePage, PlanStartDateDetails(earliestDate, validAnswer.toString)).success.value
          .set(AmendPlanStartDatePage, amendedValidAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          val request = postRequestWithDate(amendedValidAnswer)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
        }
      }


      "must redirect to Journey Recovery for a POST if the earliest payment date cannot be obtained and the data is valid" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val validDate = LocalDate.now()

        val postRequest = FakeRequest(POST, amendStartDateRoutePost)
          .withFormUrlEncodedBody(
            "value.day" -> validDate.getDayOfMonth.toString,
            "value.month" -> validDate.getMonthValue.toString,
            "value.year" -> validDate.getYear.toString
          )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NationalDirectDebitService].toInstance(mockService)
            )
            .build()

        running(application) {
          val result = route(application, postRequest).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
