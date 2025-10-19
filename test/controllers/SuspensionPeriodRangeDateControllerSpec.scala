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
import pages.SuspensionPeriodRangeDatePage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import utils.MaskAndFormatUtils.formatAmount
import views.html.SuspensionPeriodRangeDateView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class SuspensionPeriodRangeDateControllerSpec extends SpecBase with MockitoSugar {

  implicit private val messages: Messages = stubMessages()
  private val formProvider = new SuspensionPeriodRangeDateFormProvider()
  private def form = formProvider()

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
    emptyUserAnswers.set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse).success.value

  private val userAnswersWithSinglePlan =
    emptyUserAnswers.set(PaymentPlanDetailsQuery, singlePaymentPlanResponse).success.value

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
      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan)).build()

      running(application) {
        val request = getRequest()
        val result = route(application, request).value

        val view = application.injector.instanceOf[SuspensionPeriodRangeDateView]
        val planReference = "PP123456"
        val paymentAmount = formatAmount(BigDecimal(100))

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, planReference, paymentAmount)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for GET with non-Budget plan" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSinglePlan)).build()

      running(application) {
        val result = route(application, getRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on GET when previously answered" in {
      val userAnswers = userAnswersWithBudgetPlan
        .set(SuspensionPeriodRangeDatePage, validAnswer)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = getRequest()
        val result = route(application, request).value

        val view = application.injector.instanceOf[SuspensionPeriodRangeDateView]
        val planReference = "PP123456"
        val paymentAmount = formatAmount(BigDecimal(100))

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(validAnswer), NormalMode, planReference, paymentAmount)(request, messages(application)).toString
      }
    }

    "must redirect to next page when valid POST with BudgetPaymentPlan" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan))
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

    "must redirect to Journey Recovery for POST with non-Budget plan" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithSinglePlan))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when invalid POST data submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithBudgetPlan)).build()

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
          view(boundForm, NormalMode, planReference, paymentAmount)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for GET if no data exists" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for POST if no data exists" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
