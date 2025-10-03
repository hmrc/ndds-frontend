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
import forms.AmendPlanEndDateFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, NewAmendPaymentAmountPage, NewAmendPlanEndDatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{PaymentPlanTypeQuery, PaymentPlansCountQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.AmendPlanEndDateView

import java.time.LocalDate
import scala.concurrent.Future

class AmendPlanEndDateControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new AmendPlanEndDateFormProvider()
  private def form = formProvider()

  val budgetPlan = "budgetPaymentPlan"

  def onwardRoute = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.of(2025, 2, 1)
  val newValidAnswer: LocalDate = LocalDate.of(2025, 2, 2)

  lazy val amendPlanEndDateRoute = routes.AmendPlanEndDateController.onPageLoad(NormalMode).url
  lazy val amendPlanEndDateRoutePost = routes.AmendPlanEndDateController.onSubmit(NormalMode).url
  lazy val amendPaymentAmountRoute = routes.AmendPaymentAmountController.onPageLoad(NormalMode).url

  /** TODO TO be replaced with PP2 route */
  lazy val tempProblemPage = routes.JourneyRecoveryController.onPageLoad().url

  override val emptyUserAnswers = UserAnswers(userAnswersId)

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, amendPlanEndDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendPlanEndDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  def postRequestWithDate(date: LocalDate): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendPlanEndDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

  val mockService = mock[NationalDirectDebitService]
  val mockSessionRepository = mock[SessionRepository]

  "AmendPlanEndDate Controller" - {
    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val result = route(application, getRequest()).value

          val view = application.injector.instanceOf[AmendPlanEndDateView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, Call("GET", amendPaymentAmountRoute))(getRequest(), messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(AmendPlanEndDatePage, validAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanEndDateView]

          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer),
            NormalMode, Call("GET", amendPaymentAmountRoute))(getRequest(),
            messages(application)).toString
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
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "onSubmit" - {

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val request =
          FakeRequest(POST, amendPlanEndDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[AmendPlanEndDateView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", amendPaymentAmountRoute))(request, messages(application)).toString
        }
      }

      "must return a Bad Request with amendment.noChange when no amendment is made" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, budgetPlan).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPlanEndDatePage, validAnswer).success.value
          .set(NewAmendPlanEndDatePage, validAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanEndDateView]

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
          .set(PaymentPlanTypeQuery, budgetPlan).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(NewAmendPaymentAmountPage, BigDecimal(200.00)).success.value
          .set(AmendPlanEndDatePage, validAnswer).success.value
          .set(NewAmendPlanEndDatePage, validAnswer).success.value
          .set(PaymentPlansCountQuery, 2).success.value


        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockService)
          )
          .build()

        running(application) {
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))
          when(mockService.amendmentMade(any()))
            .thenReturn(true)
          when(mockService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(true))

          val request = postRequestWithDate(validAnswer)
          val result = route(application, request).value
          
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
        }
      }

      "must redirect when amendment in date is detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, budgetPlan).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(AmendPlanEndDatePage, validAnswer).success.value
          .set(NewAmendPlanEndDatePage, newValidAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockService)
          )
          .build()

        running(application) {
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))
          when(mockService.amendmentMade(any()))
            .thenReturn(true)
          when(mockService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(true))
          
          val request = postRequestWithDate(newValidAnswer)
          val result = route(application, request).value
          
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
        }
      }

      "must redirect when amendment in amount and date detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, budgetPlan).success.value
          .set(AmendPaymentAmountPage, BigDecimal(120.00)).success.value
          .set(NewAmendPaymentAmountPage, BigDecimal(200.00)).success.value
          .set(AmendPlanEndDatePage, validAnswer).success.value
          .set(NewAmendPlanEndDatePage, newValidAnswer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockService)
          )
          .build()

        running(application) {
          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))
          when(mockService.amendmentMade(any()))
            .thenReturn(true)
          when(mockService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(true))
          
          val request = postRequestWithDate(newValidAnswer)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual tempProblemPage
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
    }
  }
}
