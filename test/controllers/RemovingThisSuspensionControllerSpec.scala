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
import forms.RemovingThisSuspensionFormProvider
import models.responses.{AmendLockResponse, DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import models.{NormalMode, PaymentPlanType}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ManagePaymentPlanTypePage, RemovingThisSuspensionConfirmationPage, RemovingThisSuspensionPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class RemovingThisSuspensionControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")
  val formProvider = new RemovingThisSuspensionFormProvider()
  val form = formProvider()

  lazy val removingThisSuspensionRoute = routes.RemovingThisSuspensionController.onPageLoad(NormalMode).url

  private val mockSessionRepository = mock[SessionRepository]
  private val mockNddService = mock[NationalDirectDebitService]

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

    "must redirect to not found page if user click browser back button from confirmation page" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(RemovingThisSuspensionConfirmationPage, true)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.BackSubmissionController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET with non-Budget Payment Plan" in {

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
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
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

    "must redirect to System Error for a GET when Budget plan but missing PaymentPlanDetails" in {

      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must call CHRIS, update session, and redirect to the next page when valid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanReferenceQuery, "PREF123")
        .success
        .value
        .set(DirectDebitReferenceQuery, "DDI123")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.submitChrisData(any())(any[HeaderCarrier])) thenReturn Future.successful(true)
      when(mockNddService.suspendPaymentPlanGuard(any())) thenReturn true
      when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, removingThisSuspensionRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

      }
    }

    "must redirect to PaymentPlanDetails page when user answer No" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanReferenceQuery, "PREF123")
        .success
        .value
        .set(DirectDebitReferenceQuery, "DDI123")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, removingThisSuspensionRoute)
          .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PaymentPlanDetailsController.onPageLoad().url

      }
    }

    "must redirect to System Error for a POST with non-Budget Payment Plan" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, singlePaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

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
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
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
        .set(PaymentPlanReferenceQuery, "PREF123")
        .success
        .value
        .set(DirectDebitReferenceQuery, "DDI123")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.suspendPaymentPlanGuard(any())).thenReturn(true)
      when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

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

    "must redirect to System Error for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removingThisSuspensionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET when non-Budget plan and missing PaymentPlanDetails" in {

      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET when missing DirectDebitReference" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanReferenceQuery, "PREF123")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.suspendPaymentPlanGuard(any()))
        .thenReturn(true)
      when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a GET when missing PaymentPlanReference" in {

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, budgetPaymentPlanResponse)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(DirectDebitReferenceQuery, "DDI123")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.suspendPaymentPlanGuard(any()))
        .thenReturn(true)
      when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, removingThisSuspensionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
