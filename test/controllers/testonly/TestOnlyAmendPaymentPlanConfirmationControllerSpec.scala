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
import controllers.testonly.routes as testOnlyRoutes
import models.responses.*
import models.{NormalMode, PaymentPlanType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Constants, DirectDebitDetailsData}
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, AmendRegularPaymentAmountSummary}
import views.html.testonly.TestOnlyAmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class TestOnlyAmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData {

  "TestOnlyAmendPaymentPlanConfirmation Controller" - {
    val mockSessionRepository = mock[SessionRepository]

    def createSummaryListForBudgetPaymentPlan(userAnswers: UserAnswers,
                                              paymentPlanDetails: PaymentPlanResponse,
                                              app: Application
                                             ): Seq[SummaryListRow] = {

      Seq(
        AmendRegularPaymentAmountSummary.row(
          userAnswers.get(RegularPaymentAmountPage),
          showChange = true,
          changeCall = Some(testOnlyRoutes.TestOnlyAmendRegularPaymentAmountController.onPageLoad(NormalMode))
        )(messages(app)),
        userAnswers.get(AmendPlanEndDatePage) match {
          case Some(endDate) =>
            AmendPlanEndDateSummary.row(
              Some(endDate),
              Constants.shortDateTimeFormatPattern,
              true
            )(messages(app))
          case None =>
            AmendPlanEndDateSummary.addRow()(messages(app))
        }
      )
    }

    def createSummaryListForSinglePaymentPlans(userAnswers: UserAnswers,
                                               paymentPlanDetails: PaymentPlanResponse,
                                               app: Application
                                              ): Seq[SummaryListRow] = {
      implicit val msgs: Messages = messages(app)

      Seq(
        AmendPaymentAmountSummary.row(
          PaymentPlanType.SinglePaymentPlan.toString,
          userAnswers.get(AmendPaymentAmountPage),
          true
        ),
        AmendPlanStartDateSummary.row(
          PaymentPlanType.SinglePaymentPlan.toString,
          userAnswers.get(AmendPlanStartDatePage),
          Constants.shortDateTimeFormatPattern,
          true
        )
      )
    }

    "onPageLoad" - {
      val mockNddService = mock[NationalDirectDebitService]
      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      "must return OK and the correct view for a GET with a Budget Payment Plan when returned from AmendRegularPaymentAmount page" in {
        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
          .success
          .value
          .set(RegularPaymentAmountPage, 150.0)
          .success
          .value
          .set(AmendPlanStartDatePage, LocalDate.now())
          .success
          .value
          .set(AmendPlanEndDatePage, LocalDate.now())
          .success
          .value
          .set(AmendRegularPaymentAmountFlag, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, mockBudgetPaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            testOnlyRoutes.TestOnlyAmendRegularPaymentAmountController.onPageLoad(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET with a Budget Payment Plan when returned from AmendPlanEndDate page" in {
        val mockBudgetPaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
          )

        val userAnswers =
          emptyUserAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
            .success
            .value
            .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
            .success
            .value
            .set(RegularPaymentAmountPage, 150.0)
            .success
            .value
            .set(AmendPlanEndDatePage, LocalDate.now())
            .success
            .value
            .set(AddPaymentPlanEndDatePage, false)
            .success
            .value
            .set(AmendPlanEndDateFlag, true)
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, mockBudgetPaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            testOnlyRoutes.TestOnlyAmendPlanEndDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET with a Budget Payment Plan when returned from ConfirmRemovePlanEndDate page" in {
        val userAnswers =
          emptyUserAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
            .success
            .value
            .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
            .success
            .value
            .set(RegularPaymentAmountPage, 150.0)
            .success
            .value
            .set(AmendPlanStartDatePage, LocalDate.now())
            .success
            .value
            .set(AmendConfirmRemovePlanEndDateFlag, true)
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows =
            createSummaryListForBudgetPaymentPlan(
              userAnswers,
              mockBudgetPaymentPlanDetailResponse,
              application
            )

          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            testOnlyRoutes.TestOnlyAmendConfirmRemovePlanEndDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to page not found if already value is submitted and click browser back from confirmation page" in {
        val userAnswers = emptyUserAnswers
          .set(AmendPaymentPlanConfirmationPage, true)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, 150.0)
          .success
          .value
          .set(AmendPlanStartDatePage, LocalDate.now())
          .success
          .value
          .set(AmendPlanEndDatePage, LocalDate.now())
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.BackSubmissionController.onPageLoad().url

        }
      }

      "must return OK and the correct view for a GET with a Single Payment Plan when returned from AmendPaymentAmount page" in {
        val mockSinglePaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
          )

        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, mockSinglePaymentPlanDetailResponse)
          .success
          .value
          .set(AmendPaymentAmountPage, 150.0)
          .success
          .value
          .set(AmendPlanStartDatePage, LocalDate.now())
          .success
          .value
          .set(AmendPaymentAmountFlag, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForSinglePaymentPlans(userAnswers, mockSinglePaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            testOnlyRoutes.TestOnlyAmendPaymentAmountController.onPageLoad(NormalMode)
          )(request, messages(application)).toString

        }
      }

      "must return OK and the correct view for a GET with a Single Payment Plan when returned from AmendPaymentDate page" in {
        val mockSinglePaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
          )

        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, mockSinglePaymentPlanDetailResponse)
          .success
          .value
          .set(AmendPaymentAmountPage, 150.0)
          .success
          .value
          .set(AmendPlanStartDatePage, LocalDate.now())
          .success
          .value
          .set(AmendPaymentDateFlag, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForSinglePaymentPlans(userAnswers, mockSinglePaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/test-only/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            testOnlyRoutes.TestOnlyAmendPlanStartDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString

        }
      }

    }

    "onSubmit" - {
      "must redirect to AmendPaymentPlanUpdateController when CHRIS submission is successful" in {
        val mockNddService = mock[NationalDirectDebitService]

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
            planType                  = "SinglePaymentPlan",
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
            paymentPlanEditable       = true
          )
        )

        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now().plusDays(1))
          .success
          .value
          .set(AmendPlanEndDatePage, java.time.LocalDate.now().plusDays(4))
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1000))
          .success
          .value
          .set(DirectDebitReferenceQuery, directDebitReference)
          .success
          .value
          .set(PaymentPlanReferenceQuery, "paymentPlanReference")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockNddService), bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(true))
          when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))
          when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(DuplicateCheckResponse(false)))
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecoveryController when CHRIS submission fails" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.submitChrisData(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(false))
        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
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
          .overrides(bind[NationalDirectDebitService].toInstance(mockNddService))
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
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[SessionRepository].toInstance(mockSessionRepository)
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
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[SessionRepository].toInstance(mockSessionRepository)
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

      "must redirect to Amend payment plan update controller (AP3) when there is no amendment made" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

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
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPlanStartDatePage, java.time.LocalDate.now().plusDays(4))
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1000))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      "must redirect to Duplicate Warning controller when duplicate payment plan returns true" in {
        val mockNddService = mock[NationalDirectDebitService]

        when(mockNddService.isDuplicatePaymentPlan(any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

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
            planType                  = "SinglePaymentPlan",
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
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
          .set(PaymentPlanDetailsQuery, paymentPlanDetails)
          .success
          .value
          .set(AmendPaymentDatePage, java.time.LocalDate.now().plusDays(4))
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(100))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/test-only/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual testOnlyRoutes.TestOnlyDuplicateWarningController.onPageLoad(NormalMode).url
        }
      }

    }

  }
}
