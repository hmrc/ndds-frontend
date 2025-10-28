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
import forms.RemovingThisSuspensionFormProvider
import models.{NormalMode, PaymentPlanType}
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ManagePaymentPlanTypePage, RemovingThisSuspensionPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class RemovingThisSuspensionControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemovingThisSuspensionFormProvider()
  val form = formProvider()

  lazy val removingThisSuspensionRoute = routes.RemovingThisSuspensionController.onPageLoad(NormalMode).url

  "RemovingThisSuspension Controller" - {

    val budgetPaymentPlanResponse: PaymentPlanResponse = {
      val paymentPlanDetails = PaymentPlanDetails(
        hodService                = "sa",
        planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
        paymentReference          = "1234567890K",
        submissionDateTime        = LocalDateTime.now.minusDays(5),
        scheduledPaymentAmount    = Some(120.00),
        scheduledPaymentStartDate = Some(LocalDate.now.plusDays(5)),
        initialPaymentStartDate   = Some(LocalDate.now),
        initialPaymentAmount      = Some(BigDecimal(25.00)),
        scheduledPaymentEndDate   = Some(LocalDate.now.plusMonths(6)),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate       = Some(LocalDate.now().plusDays(5)),
        suspensionEndDate         = Some(LocalDate.now().plusDays(35)),
        balancingPaymentAmount    = Some(60.00),
        balancingPaymentDate      = Some(LocalDate.now.plusMonths(6).plusDays(10)),
        totalLiability            = None,
        paymentPlanEditable       = true
      )

      PaymentPlanResponse(
        directDebitDetails = DirectDebitDetails(
          bankSortCode       = Some("123456"),
          bankAccountNumber  = Some("12345678"),
          bankAccountName    = Some("John Doe"),
          auDdisFlag         = true,
          submissionDateTime = LocalDateTime.now.minusDays(5)
        ),
        paymentPlanDetails = paymentPlanDetails
      )
    }

    val singlePaymentPlanResponse = budgetPaymentPlanResponse.copy(
      paymentPlanDetails = budgetPaymentPlanResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
    )

    "must return OK and the correct view for a GET with Budget Payment Plan" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET with non-Budget Payment Plan" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, singlePaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(RemovingThisSuspensionPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to the next page when valid data is submitted with Budget Payment Plan" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a POST with non-Budget Payment Plan" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, singlePaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must display suspension details when payment plan has suspension" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("1234567890K")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
