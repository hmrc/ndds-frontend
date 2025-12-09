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
import forms.SuspensionPeriodRangeDateFormProvider
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import models.{NormalMode, PaymentPlanType, SuspensionPeriodRange}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ManagePaymentPlanTypePage, SuspensionPeriodRangeDatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import utils.MaskAndFormatUtils.formatAmount
import views.html.SuspensionPeriodRangeDateView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class SuspensionPeriodRangeDateControllerSpec extends SpecBase with MockitoSugar {

  implicit private val messages: Messages = stubMessages()
  private val formProvider = new SuspensionPeriodRangeDateFormProvider()

  private val PlanStartDate = Some(LocalDate.of(2025, 10, 1))
  private val planEndDate = Some(LocalDate.of(2025, 10, 10))
  private val earliestStartDateMock = LocalDate.now().plusDays(3)

  private def form = formProvider(PlanStartDate, planEndDate, earliestStartDateMock)

  private val onwardRoute = Call("GET", "/foo")

  private val startDate = LocalDate.of(2025, 10, 1)
  private val endDate = LocalDate.of(2025, 10, 10)
  private val validAnswer = SuspensionPeriodRange(startDate, endDate)

  private lazy val suspensionPeriodRangeDateRoute =
    routes.SuspensionPeriodRangeDateController.onPageLoad(NormalMode).url

  private val budgetPaymentPlanResponse = PaymentPlanResponse(
    directDebitDetails = DirectDebitDetails(
      bankSortCode       = Some("12-34-56"),
      bankAccountNumber  = Some("12345678"),
      bankAccountName    = Some("Test User"),
      auDdisFlag         = false,
      submissionDateTime = LocalDateTime.now
    ),
    paymentPlanDetails = PaymentPlanDetails(
      hodService                = "SA",
      planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
      paymentReference          = "PP123456",
      submissionDateTime        = LocalDateTime.now,
      scheduledPaymentAmount    = Some(BigDecimal(100)),
      scheduledPaymentStartDate = Some(LocalDate.now),
      initialPaymentStartDate   = None,
      initialPaymentAmount      = None,
      scheduledPaymentEndDate   = None,
      scheduledPaymentFrequency = Some("Monthly"),
      suspensionStartDate       = None,
      suspensionEndDate         = None,
      balancingPaymentAmount    = None,
      balancingPaymentDate      = None,
      totalLiability            = None,
      paymentPlanEditable       = true
    )
  )

  private val singlePaymentPlanResponse = budgetPaymentPlanResponse.copy(
    paymentPlanDetails = budgetPaymentPlanResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
  )

  private val userAnswersWithBudgetPlan =
    emptyUserAnswers
      .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
      .success
      .value
      .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
      .success
      .value

  private val userAnswersWithSinglePlan =
    emptyUserAnswers
      .set(PaymentPlanDetailsQuery, singlePaymentPlanResponse)
      .success
      .value
      .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
      .success
      .value

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, suspensionPeriodRangeDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, suspensionPeriodRangeDateRoute)
      .withFormUrlEncodedBody(
        "suspensionPeriodRangeStartDate.day"   -> startDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> startDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> startDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> endDate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> endDate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> endDate.getYear.toString
      )

  "SuspensionPeriodRangeDateController" - {

    "must return OK and the correct view for GET with BudgetPaymentPlan" in {
      val mockNddsService = mock[NationalDirectDebitService]

      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)
      when(mockNddsService.earliestSuspendStartDate(any())(any()))
        .thenReturn(Future.successful(LocalDate.now().plusDays(3)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      running(application) {
        when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)
        val request = getRequest()
        val result = route(application, request).value

        val view = application.injector.instanceOf[SuspensionPeriodRangeDateView]
        val planReference = "PP123456"
        val paymentAmount = formatAmount(BigDecimal(100))

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, planReference, paymentAmount, "12-12-2025", "3-3-2026")(request, messages(application)).toString
      }
    }

    "must redirect to System Error for GET with non-Budget plan" in {
      val mockNddsService = mock[NationalDirectDebitService]
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSinglePlan))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      running(application) {
        when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(false)
        val result = route(application, getRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must populate the view correctly on GET when previously answered" in {
      val mockNddsService = mock[NationalDirectDebitService]
      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)
      when(mockNddsService.earliestSuspendStartDate(any())(any()))
        .thenReturn(Future.successful(LocalDate.now().plusDays(3)))
      val userAnswers = userAnswersWithBudgetPlan
        .set(SuspensionPeriodRangeDatePage, validAnswer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      running(application) {
        val request = getRequest()
        val result = route(application, request).value

        val view = application.injector.instanceOf[SuspensionPeriodRangeDateView]
        val planReference = "PP123456"
        val paymentAmount = formatAmount(BigDecimal(100))

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(validAnswer), NormalMode, planReference, paymentAmount, "12-12-2025", "3-3-2026")(request, messages(application)).toString
      }
    }

    "must redirect to next page when valid POST with BudgetPaymentPlan" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNddsService = mock[NationalDirectDebitService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)
      when(mockNddsService.earliestSuspendStartDate(any())(any()))
        .thenReturn(Future.successful(LocalDate.now().plusDays(3)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockNddsService)
          )
          .build()

      val startDate = LocalDate.now().plusDays(4)
      val endDate = LocalDate.now().plusDays(10)

      val validPostRequest = FakeRequest(POST, suspensionPeriodRangeDateRoute)
        .withFormUrlEncodedBody(
          "suspensionPeriodRangeStartDate.day"   -> startDate.getDayOfMonth.toString,
          "suspensionPeriodRangeStartDate.month" -> startDate.getMonthValue.toString,
          "suspensionPeriodRangeStartDate.year"  -> startDate.getYear.toString,
          "suspensionPeriodRangeEndDate.day"     -> endDate.getDayOfMonth.toString,
          "suspensionPeriodRangeEndDate.month"   -> endDate.getMonthValue.toString,
          "suspensionPeriodRangeEndDate.year"    -> endDate.getYear.toString
        )

      running(application) {
        val result = route(application, validPostRequest).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to System Error for POST with non-Budget plan" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNddsService = mock[NationalDirectDebitService]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddsService.earliestSuspendStartDate(any())(any()))
        .thenReturn(Future.successful(LocalDate.now().plusDays(3)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithSinglePlan))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockNddsService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid POST data submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNddsService = mock[NationalDirectDebitService]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddsService.earliestSuspendStartDate(any())(any()))
        .thenReturn(Future.successful(LocalDate.now().plusDays(3)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockNddsService)
        )
        .build()

      val request =
        FakeRequest(POST, suspensionPeriodRangeDateRoute)
          .withFormUrlEncodedBody("suspensionPeriodRangeStartDate.day" -> "invalid")

      running(application) {
        val boundForm = form.bind(Map("suspensionPeriodRangeStartDate.day" -> "invalid"))
        val view = application.injector.instanceOf[SuspensionPeriodRangeDateView]
        val planReference = "PP123456"
        val paymentAmount = formatAmount(BigDecimal(100))

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(boundForm, NormalMode, planReference, paymentAmount, "12-12-2025", "3-3-2026")(request, messages(application)).toString
      }
    }

    "must redirect to System Error for GET if no data exists" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for POST if no data exists" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
