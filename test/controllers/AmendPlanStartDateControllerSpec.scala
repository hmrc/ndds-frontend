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
import models.responses.*
import models.{NormalMode, PaymentPlanType, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import views.html.AmendPlanStartDateView

import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import scala.concurrent.Future

class AmendPlanStartDateControllerSpec extends SpecBase with MockitoSugar {
  private implicit val messages: Messages = stubMessages()

  private val fixedDate = LocalDate.of(2025, 8, 6)
  private val fixedClock = Clock.fixed(
    fixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant,
    ZoneId.systemDefault()
  )

  private val formProvider = new AmendPlanStartDateFormProvider(fixedClock)
  private def form = formProvider()
  val validAnswer: LocalDate = LocalDate.now()
  val singlePlan: String = "Single Payment"

  lazy val amendPlanStartDateRoute: String = routes.AmendPlanStartDateController.onPageLoad(NormalMode).url
  lazy val amendPlanStartDateRoutePost: String = routes.AmendPlanStartDateController.onSubmit(NormalMode).url
  lazy val amendingPaymentPlanRoute: String = routes.AmendingPaymentPlanController.onPageLoad().url
  lazy val planConfirmationPage: String = routes.AmendPaymentPlanConfirmationController.onPageLoad().url

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, amendPlanStartDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendPlanStartDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )

  def postRequestWithDate(date: LocalDate): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, amendPlanStartDateRoutePost)
      .withFormUrlEncodedBody(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

  val date: LocalDateTime = LocalDateTime.now(fixedClock)

  private val earliestPaymentDate = EarliestPaymentDate("2025-02-06")

  private val formattedDateNumeric = "06 02 2025"

  "AmendPlanStartDate Controller" - {
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
          when(mockService.isSinglePaymentPlan(any())).thenReturn(true)
          when(mockService.calculateFutureWorkingDays(any(), any())(any()))
            .thenReturn(Future.successful(earliestPaymentDate))

          val result = route(application, getRequest()).value
          val view = application.injector.instanceOf[AmendPlanStartDateView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, formattedDateNumeric, Call("GET", amendingPaymentPlanRoute))(getRequest(),
                                                                                                                                messages(application)
                                                                                                                               ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = emptyUserAnswers
          .set(AmendPlanStartDatePage, validAnswer)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          val view = application.injector.instanceOf[AmendPlanStartDateView]
          val result = route(application, getRequest()).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, formattedDateNumeric, Call("GET", amendingPaymentPlanRoute))(
            getRequest(),
            messages(application)
          ).toString
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
        hodService                = "TC",
        planType                  = "Budget payment",
        paymentReference          = "987654321K",
        submissionDateTime        = LocalDateTime.now(),
        scheduledPaymentAmount    = Some(BigDecimal(1500)),
        scheduledPaymentStartDate = Some(LocalDate.now()),
        scheduledPaymentEndDate   = Some(LocalDate.now()),
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
          FakeRequest(POST, amendPlanStartDateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        running(application) {
          when(mockService.isSinglePaymentPlan(any())).thenReturn(true)
          when(mockService.calculateFutureWorkingDays(any(), any())(any()))
            .thenReturn(Future.successful(earliestPaymentDate))
          val boundForm = form.bind(Map("value" -> "invalid value"))
          val view = application.injector.instanceOf[AmendPlanStartDateView]
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, formattedDateNumeric, Call("GET", amendingPaymentPlanRoute))(
            request,
            messages(application)
          ).toString
        }
      }

      "must return to next page when payment plan start date is updated" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, singlePlan)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value
          .set(AmendPlanStartDatePage, validAnswer.plusDays(3))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(application) {
          when(mockService.isSinglePaymentPlan(any())).thenReturn(true)
          when(mockService.calculateFutureWorkingDays(any(), any())(any()))
            .thenReturn(Future.successful(earliestPaymentDate))
          val request = postRequestWithDate(validAnswer.plusDays(3))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual planConfirmationPage
        }
      }

      "must redirect to System Error for a POST if invalid payment plan type selected" in {
        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          when(mockService.isSinglePaymentPlan(any())).thenReturn(false)

          val controller = application.injector.instanceOf[AmendPaymentPlanUpdateController]
          val request = FakeRequest(GET, amendPlanStartDateRoute)
          val result = controller.onPageLoad()(request)
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
        }
      }

    }
  }
}
