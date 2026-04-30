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
import controllers.AmendPaymentPlanConfirmationControllerSpec.BudgetPaymentPlanAmendStartDateTestCase
import models.audits.AmendPaymentPlanAudit
import models.requests.ChrisSubmissionRequest
import models.responses.*
import models.{DirectDebitSource, NormalMode, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, UserAnswers, YourBankDetailsWithAuddisStatus}
import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{CurrentPageQuery, DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Constants, DirectDebitDetailsData}
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, AmendRegularPaymentAmountSummary}
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class AmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData {

  "AmendPaymentPlanConfirmation Controller" - {
    val mockSessionRepository = mock[SessionRepository]

    def createSummaryListForBudgetPaymentPlan(userAnswers: UserAnswers,
                                              paymentPlanDetails: PaymentPlanResponse,
                                              app: Application
                                             ): Seq[SummaryListRow] = {

      Seq(
        AmendRegularPaymentAmountSummary.row(
          userAnswers.get(AmendPaymentAmountPage),
          showChange = true,
          changeCall = Some(routes.AmendRegularPaymentAmountController.onPageLoad(NormalMode))
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

    "onPageLoad" ignore {
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
          .set(AmendPaymentAmountPage, 150.0)
          .success
          .value
          .set(AmendPlanStartDatePage, LocalDate.now())
          .success
          .value
          .set(AmendPlanEndDatePage, LocalDate.now())
          .success
          .value
          .set(CurrentPageQuery, "/direct-debits/amend-regular-payment-amount")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, mockBudgetPaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) must include(
            s"""href="${routes.AmendRegularPaymentAmountController.onPageLoad(NormalMode).url}""""
          )
          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            routes.AmendRegularPaymentAmountController.onPageLoad(NormalMode)
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
            .set(AmendPaymentAmountPage, 150.0)
            .success
            .value
            .set(AmendPlanEndDatePage, LocalDate.now())
            .success
            .value
            .set(AddPaymentPlanEndDatePage, false)
            .success
            .value
            .set(CurrentPageQuery, "/direct-debits/amend-payment-plan-end-date")
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, mockBudgetPaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) must include(
            s"""href="${routes.AmendPlanEndDateController.onPageLoad(NormalMode).url}""""
          )

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            routes.AmendPlanEndDateController.onPageLoad(NormalMode)
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
            .set(AmendPaymentAmountPage, 150.0)
            .success
            .value
            .set(AmendPlanStartDatePage, LocalDate.now())
            .success
            .value
            .set(CurrentPageQuery, "/direct-debits/removing-plan-end-date")
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
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

          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]

          status(result) mustEqual OK
          contentAsString(result) must include(
            s"""href="${routes.AmendConfirmRemovePlanEndDateController.onPageLoad(NormalMode).url}""""
          )

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            routes.AmendConfirmRemovePlanEndDateController.onPageLoad(NormalMode)
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
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
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
          .set(CurrentPageQuery, "/direct-debits/amend-plan-how-much-do-you-want-to-pay")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForSinglePaymentPlans(userAnswers, mockSinglePaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) must include(
            s"""href="${routes.AmendPaymentAmountController.onPageLoad(NormalMode).url}""""
          )

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            routes.AmendPaymentAmountController.onPageLoad(NormalMode)
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
          .set(CurrentPageQuery, "/direct-debits/amend-plan-payment-date")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          when(mockNddService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForSinglePaymentPlans(userAnswers, mockSinglePaymentPlanDetailResponse, application)
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)
          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK
          contentAsString(result) must include(
            s"""href="${routes.AmendPlanStartDateController.onPageLoad(NormalMode).url}""""
          )

          contentAsString(result) mustEqual view(
            NormalMode,
            summaryListRows,
            routes.AmendPlanStartDateController.onPageLoad(NormalMode)
          )(request, messages(application)).toString

        }
      }

      "must redirect to System Error for a GET when amend guard failed" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(AmendPaymentPlanConfirmationPage, false)
          .success
          .value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[NationalDirectDebitService].toInstance(mockNddService))
            .build()

        running(application) {

          when(mockNddService.amendPaymentPlanGuard(any()))
            .thenReturn(false)
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(GET, "/check-amendment-details")
          val result = controller.onPageLoad(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
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
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
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

          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url
        }
      }

      Seq(
        // 15th April 2026 is a Wednesday, the next Wednesday is 22nd April 2026
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is weekly, plan collects on Weds, calculated earliestPaymentDate is not on Weds",
          "2026-04-15",
          PaymentsFrequency.Weekly,
          "2026-04-16",
          "2026-04-22"
        ),
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is weekly, plan collects on Weds, calculated earliestPaymentDate is equal to original start date",
          "2026-04-15",
          PaymentsFrequency.Weekly,
          "2026-04-15",
          "2026-04-15"
        ),
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is weekly, plan collects on Weds, calculated earliestPaymentDate is exactly 1 week after original start date",
          "2026-04-15",
          PaymentsFrequency.Weekly,
          "2026-04-22",
          "2026-04-22"
        ),
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is monthly, plan collects on 15th, calculated earliestPaymentDate is before 15th of same month",
          "2026-04-15",
          PaymentsFrequency.Monthly,
          "2026-04-14",
          "2026-04-15"
        ),
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is monthly, plan collects on 15th, calculated earliestPaymentDate is on 15th of same month",
          "2026-04-15",
          PaymentsFrequency.Monthly,
          "2026-04-14",
          "2026-04-15"
        ),
        BudgetPaymentPlanAmendStartDateTestCase(
          "paymentFreq is monthly, plan collects on 15th, calculated earliestPaymentDate is after 15th of same month",
          "2026-04-15",
          PaymentsFrequency.Monthly,
          "2026-04-16",
          "2026-05-15"
        )
      ).foreach {
        case BudgetPaymentPlanAmendStartDateTestCase(description, originalStartDate, frequency, earliestPaymentDate, expectedNewStartDate) =>
          "must amend the start date of budget payment plans when" - {

            description in {
              val mockNddService = mock[NationalDirectDebitService]

              when(mockNddService.getFutureWorkingDays(any(), any())(any()))
                .thenReturn(Future.successful(Some(EarliestPaymentDate(earliestPaymentDate.toString))))

              val directDebitReference = "DDI123456789"
              val paymentPlanDetails = models.responses.PaymentPlanResponse(
                directDebitDetails = models.responses.DirectDebitDetails(
                  bankSortCode       = Some("123456"),
                  bankAccountNumber  = Some("12345678"),
                  bankAccountName    = Some("Bank Ltd"),
                  auDdisFlag         = true,
                  submissionDateTime = java.time.LocalDateTime.now().plusDays(5)
                ),
                paymentPlanDetails = models.responses.PaymentPlanDetails(
                  hodService                = "CESA",
                  planType                  = "BudgetPaymentPlan",
                  paymentReference          = "paymentReference",
                  submissionDateTime        = java.time.LocalDateTime.now(),
                  scheduledPaymentAmount    = Some(1000),
                  scheduledPaymentStartDate = Some(originalStartDate),
                  initialPaymentStartDate   = Some(java.time.LocalDate.now().plusDays(1)),
                  initialPaymentAmount      = Some(150),
                  scheduledPaymentEndDate   = Some(java.time.LocalDate.now().plusMonths(10)),
                  scheduledPaymentFrequency = Some(frequency.toString),
                  suspensionStartDate       = Some(java.time.LocalDate.now().plusDays(2)),
                  suspensionEndDate         = Some(java.time.LocalDate.now().plusDays(3)),
                  balancingPaymentAmount    = None,
                  balancingPaymentDate      = Some(java.time.LocalDate.now().plusDays(4)),
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

                val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
                val request = FakeRequest(POST, "/check-amendment-details")
                val result = controller.onSubmit(NormalMode)(request)

                status(result) mustBe SEE_OTHER
                redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url

                val expectedChrisSubmissionRequest =
                  ChrisSubmissionRequest(
                    serviceType                     = DirectDebitSource.SA,
                    paymentPlanType                 = PaymentPlanType.BudgetPaymentPlan,
                    paymentFrequency                = Some(frequency.toString),
                    paymentPlanReferenceNumber      = Some("paymentPlanReference"),
                    yourBankDetailsWithAuddisStatus = YourBankDetailsWithAuddisStatus("Bank Ltd", "123456", "12345678", true, true),
                    planStartDate =
                      Some(PlanStartDateDetails(enteredDate = expectedNewStartDate, earliestPlanStartDate = expectedNewStartDate.toString)),
                    planEndDate               = Some(java.time.LocalDate.now().plusDays(4)),
                    paymentDate               = None,
                    yearEndAndMonth           = None,
                    ddiReferenceNo            = directDebitReference,
                    paymentReference          = "paymentReference",
                    totalAmountDue            = Some(300),
                    paymentAmount             = None,
                    regularPaymentAmount      = None,
                    amendPaymentAmount        = Some(BigDecimal(1000)),
                    calculation               = None,
                    suspensionPeriodRangeDate = None,
                    amendPlan                 = true,
                    auditType                 = Some(AmendPaymentPlanAudit)
                  )

                verify(mockNddService).submitChrisData(equalTo(expectedChrisSubmissionRequest))(any())

              }
            }

          }

      }

      "must redirect to SystemErrorController when CHRIS submission fails" in {
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
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect to SystemErrorController when DirectDebitReference is missing" in {
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
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect to SystemErrorController when PaymentPlanReference is missing" in {
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
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
        }
      }

      "must redirect to Amend payment plan update controller (AP3) when there is no amendment made" in {

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
            scheduledPaymentAmount    = Some(1200),
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
          .set(AmendPlanEndDatePage, java.time.LocalDate.now().plusMonths(10))
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1200))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendPaymentPlanUpdateController.onPageLoad().url
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
          val controller = application.injector.instanceOf[AmendPaymentPlanConfirmationController]
          val request = FakeRequest(POST, "/check-amendment-details")
          val result = controller.onSubmit(NormalMode)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.AmendDuplicateWarningController.onPageLoad(NormalMode).url
        }
      }

    }

  }

}

object AmendPaymentPlanConfirmationControllerSpec {

  final case class BudgetPaymentPlanAmendStartDateTestCase(
    description: String,
    originalStartDate: LocalDate,
    paymentFrequency: PaymentsFrequency,
    earliestPaymentDate: LocalDate,
    expectedNewStartDate: LocalDate
  )

  object BudgetPaymentPlanAmendStartDateTestCase {
    def apply(description: String,
              originalStartDate: String,
              paymentFrequency: PaymentsFrequency,
              earliestPaymentDate: String,
              expectedNewStartDate: String
             ): BudgetPaymentPlanAmendStartDateTestCase =
      BudgetPaymentPlanAmendStartDateTestCase(
        description,
        LocalDate.parse(originalStartDate),
        paymentFrequency,
        LocalDate.parse(earliestPaymentDate),
        LocalDate.parse(expectedNewStartDate)
      )
  }

}
