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
import forms.DuplicateWarningFormProvider
import models.responses.AmendLockResponse
import models.{NormalMode, PaymentPlanType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{DuplicateWarningPage, ManagePaymentPlanTypePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import views.html.DuplicateWarningView

import scala.concurrent.Future

class DuplicateWarningControllerSpec extends SpecBase {

  private val formProvider = new DuplicateWarningFormProvider()
  private val form = formProvider()
  private val mode = NormalMode

  "DuplicateWarningController" - {
    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "onPageLoad" - {
      "must return OK and view DuplicateWarningController onPageLoad" in {
        val ua = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(ua))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(ua)))

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(GET, routes.DuplicateWarningController.onPageLoad(mode).url)
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[DuplicateWarningView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            mode,
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
          )(request, messages(application)).toString
        }
      }

      "must redirect to page not found if already value is submitted and click browser back from Updated page" in {
        val ua =
          emptyUserAnswers
            .set(DuplicateWarningPage, true)
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(ua)))

          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(GET, routes.DuplicateWarningController.onPageLoad(mode).url)
          val result = controller.onPageLoad(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.BackSubmissionController.onPageLoad().url
        }
      }

      "must redirect to System Error page when amend payment plan guard returns false" in {
        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(GET, routes.DuplicateWarningController.onPageLoad(mode).url)
          val result = controller.onPageLoad(mode)(request)
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must redirect to AmendPaymentPlanUpdateController when user selects Yes (true) and chRIS submission successful" in {
        val directDebitReference = "DDI123456789"

        val paymentPlanDetails = models.responses.PaymentPlanResponse(
          directDebitDetails = models.responses.DirectDebitDetails(
            bankSortCode       = Some("123456"),
            bankAccountNumber  = Some("12345678"),
            bankAccountName    = Some("Bank Ltd"),
            auDdisFlag         = true,
            submissionDateTime = java.time.LocalDateTime.now()
          ),
          paymentPlanDetails = models.responses.PaymentPlanDetails(
            hodService                = "CESA",
            planType                  = "BudgetPaymentPlan",
            paymentReference          = "paymentReference",
            submissionDateTime        = java.time.LocalDateTime.now(),
            scheduledPaymentAmount    = Some(1000),
            scheduledPaymentStartDate = Some(java.time.LocalDate.now().plusDays(4)),
            initialPaymentStartDate   = Some(java.time.LocalDate.now()),
            initialPaymentAmount      = Some(150),
            scheduledPaymentEndDate   = Some(java.time.LocalDate.now().plusMonths(10)),
            scheduledPaymentFrequency = Some("Monthly"),
            suspensionStartDate       = Some(java.time.LocalDate.now()),
            suspensionEndDate         = Some(java.time.LocalDate.now()),
            balancingPaymentAmount    = None,
            balancingPaymentDate      = Some(java.time.LocalDate.now()),
            totalLiability            = Some(300),
            paymentPlanEditable       = false
          )
        )

        val ua = emptyUserAnswers
          .set(DuplicateWarningPage, true)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(PaymentPlanReferenceQuery, "PPREF123")
          .success
          .value

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockService.submitChrisData(any())(any())) thenReturn Future.successful(true)
        when(mockService.lockPaymentPlan(any(), any())(any())) thenReturn Future.successful(AmendLockResponse(true))

        val application = applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(POST, "/amend-already-have-payment-plan").withFormUrlEncodedBody("value" -> "true")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      "must redirect to System Error page when user selects Yes (true) but chRIS submission fails" in {
        val directDebitReference = "DDI123456789"

        val paymentPlanDetails = models.responses.PaymentPlanResponse(
          directDebitDetails = models.responses.DirectDebitDetails(
            bankSortCode       = Some("123456"),
            bankAccountNumber  = Some("12345678"),
            bankAccountName    = Some("Bank Ltd"),
            auDdisFlag         = true,
            submissionDateTime = java.time.LocalDateTime.now()
          ),
          paymentPlanDetails = models.responses.PaymentPlanDetails(
            hodService                = "CESA",
            planType                  = "BudgetPaymentPlan",
            paymentReference          = "paymentReference",
            submissionDateTime        = java.time.LocalDateTime.now(),
            scheduledPaymentAmount    = Some(1000),
            scheduledPaymentStartDate = Some(java.time.LocalDate.now().plusDays(4)),
            initialPaymentStartDate   = Some(java.time.LocalDate.now()),
            initialPaymentAmount      = Some(150),
            scheduledPaymentEndDate   = Some(java.time.LocalDate.now().plusMonths(10)),
            scheduledPaymentFrequency = Some("Monthly"),
            suspensionStartDate       = Some(java.time.LocalDate.now()),
            suspensionEndDate         = Some(java.time.LocalDate.now()),
            balancingPaymentAmount    = None,
            balancingPaymentDate      = Some(java.time.LocalDate.now()),
            totalLiability            = Some(300),
            paymentPlanEditable       = false
          )
        )

        val ua = emptyUserAnswers
          .set(DuplicateWarningPage, true)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(PaymentPlanReferenceQuery, "PPREF123")
          .success
          .value

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockService.submitChrisData(any())(any())) thenReturn Future.successful(false)
        when(mockService.lockPaymentPlan(any(), any())(any())) thenReturn Future.successful(AmendLockResponse(true))

        val application = applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(POST, "/amend-already-have-payment-plan").withFormUrlEncodedBody("value" -> "true")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect to AmendPaymentPlanConfirmationController when user selects No (false)" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val ua = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(ua))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

          val controller = application.injector.instanceOf[DuplicateWarningController]
          val request = FakeRequest(POST, routes.DuplicateWarningController.onSubmit(mode).url).withFormUrlEncodedBody(("value", "false"))
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanConfirmationController.onPageLoad().url
        }
      }

    }
  }
}
