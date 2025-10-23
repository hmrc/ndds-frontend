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
import models.PaymentPlanType
import models.responses.PaymentPlanResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.checkAnswers.{SuspensionPeriodRangeDateSummary, *}
import views.html.PaymentPlanDetailsView

import java.time.LocalDateTime
import scala.concurrent.Future

class PaymentPlanDetailsControllerSpec extends SpecBase {

  "PaymentPlanDetails Controller" - {
    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    def varRepaySummaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
      val planDetail = paymentPlanData.paymentPlanDetails
      Seq(
        AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
        AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
        DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
        TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
        MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability)(messages(app)),
        FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability)(messages(app)),
        AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(messages(app)),
        AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app))
      )
    }

    "onPageLoad" - {
      ".SinglePayment Plan" - {
        "must return OK and the correct view for a GET" in {
          def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
            val planDetail = paymentPlanData.paymentPlanDetails
            Seq(
              AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
              AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
              DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
              AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
              AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
                messages(app)
              )
            )
          }

          val mockSinglePaymentPlanDetailResponse =
            dummyPlanDetailResponse.copy(paymentPlanDetails =
              dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
            )

          val paymentPlanReference = "ppReference"
          val directDebitReference = "ddReference"

          val userAnswersWithPaymentReference =
            emptyUserAnswers
              .set(
                PaymentPlanReferenceQuery,
                paymentPlanReference
              )
              .success
              .value
              .set(
                DirectDebitReferenceQuery,
                directDebitReference
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NationalDirectDebitService].toInstance(mockService)
            )
            .build()

          running(application) {
            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))
            when(mockSessionRepository.get(any()))
              .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
            when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
              .thenReturn(Future.successful(mockSinglePaymentPlanDetailResponse))
            when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
              .thenReturn(Future.successful(true))

            val summaryListRows = summaryList(mockSinglePaymentPlanDetailResponse, application)
            val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[PaymentPlanDetailsView]
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("singlePaymentPlan", paymentPlanReference, true, true, false, summaryListRows)(
              request,
              messages(application)
            ).toString
          }
        }
      }

      ".BudgetPayment Plan" - {

        def summaryListWithoutSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
            MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability)(messages(app)),
            FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            ),
            AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app))
          )
        }

        def summaryListWithSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
            MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount, planDetail.totalLiability)(messages(app)),
            FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount, planDetail.totalLiability)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            ),
            AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
            SuspensionPeriodRangeDateSummary.row(planDetail.suspensionStartDate, planDetail.suspensionEndDate)(messages(app))
          )
        }

        "must return OK and the correct view for a GET" - {

          "when Suspension is inactive" - {
            "should show Amend, Cancel and Suspend actions when payment plan is already started but active" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(10).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))

                val summaryListRows = summaryListWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, true, true, true, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should show Amend, Cancel and Suspend actions when payment plan is already started but scheduledPaymentEndDate is None" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(10).toLocalDate),
                    scheduledPaymentEndDate   = None,
                    suspensionStartDate       = None,
                    suspensionEndDate         = None
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))

                val summaryListRows = summaryListWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, true, true, true, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show Amend, Cancel and Suspend actions when scheduledPaymentStartDate is two day prior" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))

                val summaryListRows = summaryListWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, false, false, false, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show  Amend, Cancel and Suspend actions when scheduledPaymentEndDate is three day prior" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(10).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(3).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(false))

                val summaryListRows = summaryListWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, false, false, false, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show  Amend, Cancel and Suspend actions when payment plan is inactive" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(30).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(false))

                val summaryListRows = summaryListWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, false, false, false, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }
          }

          "when Suspension is active" - {
            "should not show Amend, Cancel and Suspend actions" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(20).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                    suspensionStartDate       = Some(LocalDateTime.now().minusDays(5).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(5).toLocalDate)
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))

                val summaryListRows = summaryListWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, true, true, false, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show Amend, Cancel and Suspend actions when payment plan is already started but scheduledPaymentEndDate is None" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(10).toLocalDate),
                    scheduledPaymentEndDate   = None,
                    suspensionStartDate       = Some(LocalDateTime.now().minusDays(5).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(5).toLocalDate)
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val userAnswersWithPaymentReference =
                emptyUserAnswers
                  .set(
                    PaymentPlanReferenceQuery,
                    paymentPlanReference
                  )
                  .success
                  .value
                  .set(
                    DirectDebitReferenceQuery,
                    directDebitReference
                  )
                  .success
                  .value

              val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[NationalDirectDebitService].toInstance(mockService)
                )
                .build()

              running(application) {
                when(mockSessionRepository.set(any()))
                  .thenReturn(Future.successful(true))
                when(mockSessionRepository.get(any()))
                  .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
                when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
                  .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(false))

                val summaryListRows = summaryListWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan", paymentPlanReference, true, true, false, summaryListRows)(
                  request,
                  messages(application)
                ).toString
              }
            }
          }
        }
      }

      ".VariablePayment Plan" - {
        "must return OK and the correct view for a GET" in {
          val mockVariablePaymentPlanDetailResponse =
            dummyPlanDetailResponse.copy(paymentPlanDetails =
              dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.VariablePaymentPlan.toString)
            )

          val paymentPlanReference = "ppReference"
          val directDebitReference = "ddReference"

          val userAnswersWithPaymentReference =
            emptyUserAnswers
              .set(
                PaymentPlanReferenceQuery,
                paymentPlanReference
              )
              .success
              .value
              .set(
                DirectDebitReferenceQuery,
                directDebitReference
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NationalDirectDebitService].toInstance(mockService)
            )
            .build()

          running(application) {
            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))
            when(mockSessionRepository.get(any()))
              .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
            when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
              .thenReturn(Future.successful(mockVariablePaymentPlanDetailResponse))
            when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
              .thenReturn(Future.successful(true))
            when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
              .thenReturn(Future.successful(true))

            val summaryListRows = varRepaySummaryList(mockVariablePaymentPlanDetailResponse, application)
            val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[PaymentPlanDetailsView]
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("variablePaymentPlan", paymentPlanReference, false, true, false, summaryListRows)(
              request,
              messages(application)
            ).toString
          }
        }
      }

      ".TaxCreditRepayment Plan" - {
        "must return OK and the correct view for a GET" in {
          val mockTaxCreditRepaymentPlanDetailResponse =
            dummyPlanDetailResponse.copy(paymentPlanDetails =
              dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.TaxCreditRepaymentPlan.toString)
            )

          val paymentPlanReference = "ppReference"
          val directDebitReference = "ddReference"

          val userAnswersWithPaymentReference =
            emptyUserAnswers
              .set(
                PaymentPlanReferenceQuery,
                paymentPlanReference
              )
              .success
              .value
              .set(
                DirectDebitReferenceQuery,
                directDebitReference
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[NationalDirectDebitService].toInstance(mockService)
            )
            .build()

          running(application) {
            when(mockSessionRepository.set(any()))
              .thenReturn(Future.successful(true))
            when(mockSessionRepository.get(any()))
              .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
            when(mockService.getPaymentPlanDetails(any(), any())(any(), any()))
              .thenReturn(Future.successful(mockTaxCreditRepaymentPlanDetailResponse))

            val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
            val summaryListRows = varRepaySummaryList(mockTaxCreditRepaymentPlanDetailResponse, application)
            val result = route(application, request).value
            val view = application.injector.instanceOf[PaymentPlanDetailsView]
            status(result) mustEqual OK
            contentAsString(result) mustEqual view("taxCreditRepaymentPlan", paymentPlanReference, false, false, false, summaryListRows)(
              request,
              messages(application)
            ).toString
          }
        }
      }

      "must redirect to Journey Recover page when DirectDebitReferenceQuery is not set" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides()
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onRedirect" - {
      "must redirect to Payment Plan Details page when a PaymentReferenceQuery is provided" in {
        val paymentReference = "paymentReference"
        val userAnswersWithPaymentReference =
          emptyUserAnswers
            .set(
              PaymentPlanReferenceQuery,
              paymentReference
            )
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
          val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onRedirect(paymentReference).url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PaymentPlanDetailsController.onPageLoad().url

          verify(mockSessionRepository).set(eqTo(userAnswersWithPaymentReference))
        }
      }
    }
  }
}
