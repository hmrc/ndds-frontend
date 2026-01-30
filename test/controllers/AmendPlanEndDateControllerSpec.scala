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
import models.{NextPaymentValidationResult, NormalMode, PaymentPlanType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, ManagePaymentPlanTypePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import utils.Constants
import views.html.AmendPlanEndDateView

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class AmendPlanEndDateControllerSpec extends SpecBase with MockitoSugar {
  private implicit val messages: Messages = stubMessages()
  private val formProvider = new AmendPlanEndDateFormProvider()
  private def form = formProvider()
  val validAnswer: LocalDate = LocalDate.now()

  lazy val amendPlanEndDateRoute: String = routes.AmendPlanEndDateController.onPageLoad(NormalMode).url
  lazy val amendPlanEndDateRoutePost: String = routes.AmendPlanEndDateController.onSubmit(NormalMode).url
  lazy val amendingPaymentPlanRoute: String = routes.AmendingPaymentPlanController.onPageLoad().url
  lazy val planConfirmationPage: String = routes.AmendPaymentPlanConfirmationController.onPageLoad().url

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, amendPlanEndDateRoute + "?beforeDate=2027-01-01")

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

  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)
  val beforeDate: String = LocalDate.now().plusMonths(12).format(dateFormat)

  "AmendPlanEndDate Controller" - {
    val mockService = mock[NationalDirectDebitService]
    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        val userAnswers: UserAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.isBudgetPaymentPlan(any())).thenReturn(true)

          val result = route(application, getRequest()).value
          val view = application.injector.instanceOf[AmendPlanEndDateView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, Call("GET", amendingPaymentPlanRoute), beforeDate)(getRequest(),
                                                                                                                      messages(application)
                                                                                                                     ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .set(AmendPlanEndDatePage, validAnswer)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.isBudgetPaymentPlan(any())).thenReturn(true)
          val view = application.injector.instanceOf[AmendPlanEndDateView]
          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, Call("GET", amendingPaymentPlanRoute), beforeDate)(
            getRequest(),
            messages(application)
          ).toString
        }
      }

      "must redirect to System Error for a GET if PaymentPlanType is other than SinglePaymentPlan and BudgetPaymentPlan" in {
        val userAnswers = emptyUserAnswers
          .set(AmendPlanEndDatePage, validAnswer)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.isBudgetPaymentPlan(any())).thenReturn(false)
          val result = route(application, getRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect to System Error for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          val result = route(application, getRequest()).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
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
        planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
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
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        val request = FakeRequest(POST, amendPlanEndDateRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          val boundForm = form.bind(Map("value" -> "invalid value"))
          val view = application.injector.instanceOf[AmendPlanEndDateView]
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, Call("GET", amendingPaymentPlanRoute), beforeDate)(request,
                                                                                                                           messages(application)
                                                                                                                          ).toString
        }
      }

      "must return to next page when payment plan end date is updated" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value
          .set(AmendPlanEndDatePage, LocalDate.now().plusDays(7))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.calculateNextPaymentDate(any(), any(), any())(any))
            .thenReturn(Future.successful(NextPaymentValidationResult(Some(validAnswer), nextPaymentDateValid = true)))

          val request = postRequestWithDate(validAnswer.plusDays(7))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual planConfirmationPage
        }
      }

      "must redirect to System Error for a POST when no PaymentPlanResponse exists" in {
        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, "Single payment")
          .success
          .value
          .set(AmendPlanEndDatePage, validAnswer.plusDays(3))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          val request = postRequestWithDate(validAnswer.plusDays(3))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
        }
      }
    }
  }
}
