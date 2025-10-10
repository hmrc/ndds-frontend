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
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import models.{NormalMode, PaymentPlanType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPaymentPlanTypePage, AmendPlanEndDatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import views.html.AmendPlanEndDateView

import java.time.{LocalDate, LocalDateTime}

class AmendPlanEndDateControllerSpec extends SpecBase with MockitoSugar {
  private implicit val messages: Messages = stubMessages()
  private val formProvider = new AmendPlanEndDateFormProvider()
  private def form = formProvider()
  val validAnswer: LocalDate = LocalDate.now()

  lazy val amendPlanEndDateRoute: String = routes.AmendPlanEndDateController.onPageLoad(NormalMode).url
  lazy val amendPlanEndDateRoutePost: String = routes.AmendPlanEndDateController.onSubmit(NormalMode).url
  lazy val amendPaymentAmountRoute: String = routes.AmendPaymentAmountController.onPageLoad(NormalMode).url
  lazy val planConfirmationPage: String = routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url

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
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

  "AmendPlanEndDate Controller" - {
    val mockService = mock[NationalDirectDebitService]
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
        val userAnswers = emptyUserAnswers.set(AmendPlanEndDatePage, validAnswer).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanEndDateView]
          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, Call("GET", amendPaymentAmountRoute))(getRequest(),
                                                                                                                           messages(application)
                                                                                                                          ).toString
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
      val directDebitDetails = DirectDebitDetails(
        bankSortCode       = Some("123456"),
        bankAccountNumber  = Some("12345678"),
        bankAccountName    = Some("UK Bank"),
        auDdisFlag         = true,
        submissionDateTime = LocalDateTime.now()
      )
      val planDetails = PaymentPlanDetails(
        hodService                = "SA",
        planType                  = PaymentPlanType.SinglePaymentPlan.toString,
        paymentReference          = "987654321K",
        submissionDateTime        = LocalDateTime.now(),
        scheduledPaymentAmount    = Some(BigDecimal(1500)),
        scheduledPaymentStartDate = Some(LocalDate.now()),
        scheduledPaymentEndDate   = Some(LocalDate.now().plusDays(10)),
        scheduledPaymentFrequency = Some("Monthly"),
        initialPaymentStartDate   = Some(LocalDate.now()),
        initialPaymentAmount      = Some(BigDecimal(1500)),
        suspensionStartDate       = Some(LocalDate.now()),
        suspensionEndDate         = Some(LocalDate.now()),
        balancingPaymentAmount    = Some(BigDecimal(1500)),
        balancingPaymentDate      = Some(LocalDate.now()),
        totalLiability            = Some(BigDecimal(1500)),
        paymentPlanEditable       = true
      )
      val paymentPlanResponse = PaymentPlanResponse(directDebitDetails, planDetails)

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        val request =
          FakeRequest(POST, amendPlanEndDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val boundForm = form.bind(Map("value" -> "invalid value"))
          val view = application.injector.instanceOf[AmendPlanEndDateView]
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", amendPaymentAmountRoute))(request, messages(application)).toString
        }
      }

      "must return a Bad Request when no amendment is made" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value
          .set(AmendPlanEndDatePage, paymentPlanResponse.paymentPlanDetails.scheduledPaymentEndDate.get)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val view = application.injector.instanceOf[AmendPlanEndDateView]
          val request = postRequestWithDate(validAnswer.plusDays(10))
          val result = route(application, request).value

          val errorForm = form.fill(validAnswer.plusDays(10)).withError("value", "amendment.noChange")

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual
            view(errorForm, NormalMode, Call("GET", amendPaymentAmountRoute))(request, messages(application)).toString
        }
      }

      "must return to next page when payment amount is updated" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, "Single payment")
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1900))
          .success
          .value
          .set(AmendPlanEndDatePage, validAnswer)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val request = postRequestWithDate(validAnswer)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual planConfirmationPage
        }
      }

      "must return to next page when payment plan end date is updated" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value
          .set(AmendPlanEndDatePage, paymentPlanResponse.paymentPlanDetails.scheduledPaymentEndDate.get)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val request = postRequestWithDate(validAnswer.plusDays(7))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual planConfirmationPage
        }
      }

      "must return to next page when payment amount and plan end date is updated" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, "Single payment")
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1900))
          .success
          .value
          .set(AmendPlanEndDatePage, validAnswer.plusDays(3))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val request = postRequestWithDate(validAnswer.plusDays(3))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual planConfirmationPage
        }
      }

      "must redirect to Journey Recovery for a POST if invalid payment plan type selected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, "Variable payment")
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1900))
          .success
          .value
          .set(AmendPlanEndDatePage, validAnswer.plusDays(3))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)
          val request = postRequestWithDate(validAnswer.plusDays(3))
          val result = intercept[Exception](route(application, request).value.futureValue)

          result.getMessage must include("NDDS Payment Plan Guard: Cannot amend this plan type: Variable payment")
        }
      }

      "must redirect to Journey Recovery for a POST when no amend payment plan exists" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(AmendPaymentPlanTypePage, "Single payment")
          .success
          .value
          .set(AmendPlanEndDatePage, validAnswer.plusDays(3))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          val request = postRequestWithDate(validAnswer.plusDays(3))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
