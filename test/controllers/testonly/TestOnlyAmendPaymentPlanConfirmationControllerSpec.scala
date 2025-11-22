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

package controllers.testonly

import base.SpecBase
import controllers.routes
import models.responses.*
import models.{NormalMode, PaymentPlanType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Constants, DirectDebitDetailsData}
import viewmodels.checkAnswers.*
import views.html.testonly.TestOnlyAmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class TestOnlyAmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData {

  "TestOnlyAmendPaymentPlanConfirmation Controller" - {
    val mockSessionRepository = mock[SessionRepository]

    "onPageLoad" - {
      val mockNddService = mock[NationalDirectDebitService]
      "must return OK and the correct view for a GET with a Budget Payment Plan" in {
        when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)

        val userAnswers =
          emptyUserAnswers
            .set(
              ManagePaymentPlanTypePage,
              PaymentPlanType.BudgetPaymentPlan.toString
            )
            .success
            .value
            .set(
              AmendPaymentAmountPage,
              150.0
            )
            .success
            .value
            .set(
              AmendPlanStartDatePage,
              LocalDate.now()
            )
            .success
            .value
            .set(
              AmendPlanEndDatePage,
              LocalDate.now()
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))

          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          status(result) mustEqual OK

          val html = contentAsString(result)
          html must include("Plan end date")
          html must include("Regular payment amount")
          html must include("Check your amendment details")

        }
      }

      "must redirect to page not found if already value is submitted and click browser back from confirmation page" in {
        val userAnswers =
          emptyUserAnswers
            .set(
              AmendPaymentPlanConfirmationPage,
              true
            )
            .success
            .value
            .set(
              ManagePaymentPlanTypePage,
              PaymentPlanType.BudgetPaymentPlan.toString
            )
            .success
            .value
            .set(
              AmendPaymentAmountPage,
              150.0
            )
            .success
            .value
            .set(
              AmendPlanStartDatePage,
              LocalDate.now()
            )
            .success
            .value
            .set(
              AmendPlanEndDatePage,
              LocalDate.now()
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.BackSubmissionController.onPageLoad().url

        }
      }

      "must return OK and the correct view for a GET with a Single Payment Plan" in {
        val userAnswers =
          emptyUserAnswers
            .set(
              ManagePaymentPlanTypePage,
              PaymentPlanType.SinglePaymentPlan.toString
            )
            .success
            .value
            .set(
              AmendPaymentAmountPage,
              150.0
            )
            .success
            .value
            .set(
              AmendPaymentDatePage,
              LocalDate.now()
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))

          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          status(result) mustEqual OK

          val html = contentAsString(result)
          html must include("Payment date")
          html must include("Payment amount")
          html must include("Check your amendment details")
        }
      }
    }

    "onSubmit" - {
      "must redirect to AmendPaymentPlanUpdateController when CHRIS submission is successful" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))
        when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))
        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))

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

        val userAnswers = emptyUserAnswers
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value
          .set(PaymentPlanReferenceQuery, "paymentPlanReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when CHRIS submission fails" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))
        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))

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
            balancingPaymentAmount    = Some(600),
            balancingPaymentDate      = Some(java.time.LocalDate.now()),
            totalLiability            = Some(300),
            paymentPlanEditable       = false
          )
        )

        val userAnswers = emptyUserAnswers
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value
          .set(PaymentPlanReferenceQuery, "paymentPlanReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when DirectDebitReference is missing" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))
        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))

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
            balancingPaymentAmount    = Some(600),
            balancingPaymentDate      = Some(java.time.LocalDate.now()),
            totalLiability            = Some(300),
            paymentPlanEditable       = false
          )
        )

        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value
          .set(PaymentPlanReferenceQuery, "paymentPlanReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when PaymentPlanReference is missing" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))
        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))

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
            balancingPaymentAmount    = Some(600),
            balancingPaymentDate      = Some(java.time.LocalDate.now()),
            totalLiability            = Some(300),
            paymentPlanEditable       = false
          )
        )

        val userAnswers = emptyUserAnswers
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Duplicate Warning controller when duplicate payment plan returns true" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

        val userAnswers = emptyUserAnswers
          .set(AmendPlanStartDatePage, java.time.LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.DuplicateWarningController.onPageLoad(NormalMode).url
        }
      }

    }

  }
}
