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
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class AmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData {

  "PaymentPlanDetails Controller" - {

    val mockSessionRepository = mock[SessionRepository]

    def createSummaryListForBudgetPaymentPlan(userAnswers: UserAnswers,
                                              paymentPlanDetails: PaymentPlanResponse,
                                              app: Application
                                             ): Seq[SummaryListRow] = {
      val paymentPlan = paymentPlanDetails.paymentPlanDetails

      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers.get(ManagePaymentPlanTypePage).getOrElse(""))(messages(app)),
        AmendPaymentPlanSourceSummary.row(paymentPlan.hodService)(messages(app)),
        TotalAmountDueSummary.row(paymentPlan.totalLiability)(messages(app)),
        MonthlyPaymentAmountSummary.row(paymentPlan.scheduledPaymentAmount, paymentPlan.totalLiability)(messages(app)),
        FinalPaymentAmountSummary.row(paymentPlan.balancingPaymentAmount, paymentPlan.totalLiability)(messages(app)),
        PaymentsFrequencySummary.row(paymentPlan.scheduledPaymentFrequency)(messages(app)),
        AmendPlanStartDateSummary.row(
          PaymentPlanType.BudgetPaymentPlan.toString,
          userAnswers.get(AmendPlanStartDatePage),
          Constants.shortDateTimeFormatPattern
        )(messages(app)),
        AmendPaymentAmountSummary.row(
          PaymentPlanType.BudgetPaymentPlan.toString,
          userAnswers.get(AmendPaymentAmountPage),
          true
        )(messages(app)),
        AmendPlanEndDateSummary.row(
          userAnswers.get(AmendPlanEndDatePage),
          Constants.shortDateTimeFormatPattern,
          true
        )(messages(app))
      )
    }

    def createSummaryListForSinglePaymentPlans(userAnswers: UserAnswers,
                                               paymentPlanDetails: PaymentPlanResponse,
                                               app: Application
                                              ): Seq[SummaryListRow] = {
      val paymentPlan = paymentPlanDetails.paymentPlanDetails

      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers.get(ManagePaymentPlanTypePage).getOrElse(""))(messages(app)),
        AmendPaymentPlanSourceSummary.row(paymentPlan.hodService)(messages(app)),
        DateSetupSummary.row(paymentPlan.submissionDateTime)(messages(app)),
        AmendPaymentAmountSummary.row(
          PaymentPlanType.SinglePaymentPlan.toString,
          userAnswers.get(AmendPaymentAmountPage),
          true
        )(messages(app)),
        AmendPlanStartDateSummary.row(
          PaymentPlanType.SinglePaymentPlan.toString,
          userAnswers.get(AmendPlanStartDatePage),
          Constants.shortDateTimeFormatPattern,
          true
        )(messages(app))
      )
    }

    "onPageLoad" - {
      "must return OK and the correct view for a GET with a Budget Payment Plan" in {

        val mockBudgetPaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
          )

        val directDebitReference = "122222"
        val paymentPlanReference = "paymentReference"
        val userAnswers =
          emptyUserAnswers
            .set(
              DirectDebitReferenceQuery,
              directDebitReference
            )
            .success
            .value
            .set(
              PaymentPlanReferenceQuery,
              paymentPlanReference
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
              PaymentPlanDetailsQuery,
              mockBudgetPaymentPlanDetailResponse
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
              LocalDate.now().plusMonths(2)
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

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, mockBudgetPaymentPlanDetailResponse, application)
          val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            paymentPlanReference,
            directDebitReference,
            mockBudgetPaymentPlanDetailResponse.directDebitDetails.bankSortCode.get,
            mockBudgetPaymentPlanDetailResponse.directDebitDetails.bankAccountNumber.get,
            summaryListRows,
            routes.AmendPlanEndDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET with a Single Payment Plan" in {
        val mockSinglePaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
          )

        val directDebitReference = "122222"
        val paymentPlanReference = "paymentReference"
        val userAnswers =
          emptyUserAnswers
            .set(
              DirectDebitReferenceQuery,
              directDebitReference
            )
            .success
            .value
            .set(
              PaymentPlanReferenceQuery,
              paymentPlanReference
            )
            .success
            .value
            .set(
              ManagePaymentPlanTypePage,
              PaymentPlanType.SinglePaymentPlan.toString
            )
            .success
            .value
            .set(
              PaymentPlanDetailsQuery,
              mockSinglePaymentPlanDetailResponse
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
              LocalDate.now().plusMonths(2)
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

          val summaryListRows = createSummaryListForSinglePaymentPlans(userAnswers, mockSinglePaymentPlanDetailResponse, application)
          val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            paymentPlanReference,
            directDebitReference,
            mockSinglePaymentPlanDetailResponse.directDebitDetails.bankSortCode.get,
            mockSinglePaymentPlanDetailResponse.directDebitDetails.bankAccountNumber.get,
            summaryListRows,
            routes.AmendPlanStartDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString
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
          .set(PaymentPlanReferenceQuery, "paymentReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.AmendPaymentPlanConfirmationController.onSubmit(NormalMode).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when CHRIS submission fails" in {

        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))

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
          .set(PaymentPlanReferenceQuery, "paymentReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.AmendPaymentPlanConfirmationController.onSubmit(NormalMode).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when DirectDebitReference is missing" in {

        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))

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
          .set(PaymentPlanReferenceQuery, "paymentReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.AmendPaymentPlanConfirmationController.onSubmit(NormalMode).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when PaymentPlanReference is missing" in {

        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))

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
          val request = FakeRequest(POST, routes.AmendPaymentPlanConfirmationController.onSubmit(NormalMode).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

  }
}
