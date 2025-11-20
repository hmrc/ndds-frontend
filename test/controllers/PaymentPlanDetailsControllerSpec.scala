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
import models.responses.{AdvanceNoticeResponse, PaymentPlanResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.RemovingThisSuspensionPage
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AdvanceNoticeResponseQuery, DirectDebitReferenceQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.checkAnswers.{SuspensionPeriodRangeDateSummary, *}
import views.html.PaymentPlanDetailsView

import java.text.NumberFormat
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.Future

class PaymentPlanDetailsControllerSpec extends SpecBase {

  "PaymentPlanDetails Controller" - {
    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "onPageLoad" - {
      ".SinglePayment Plan" - {

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

        "must return OK and the correct view for a GET" - {
          "should show Amend, Cancel actions but not Suspend action when scheduledPaymentStartDate is beyond 2 working days" in {
            val mockSinglePaymentPlanDetailResponse =
              dummyPlanDetailResponse.copy(paymentPlanDetails =
                dummyPlanDetailResponse.paymentPlanDetails.copy(
                  planType                  = PaymentPlanType.SinglePaymentPlan.toString,
                  scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                  scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                  suspensionStartDate       = None,
                  suspensionEndDate         = None,
                  paymentPlanEditable       = true
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
                .thenReturn(Future.successful(mockSinglePaymentPlanDetailResponse))
              when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                .thenReturn(Future.successful(true))
              when(mockService.isPaymentPlanEditable(any()))
                .thenReturn(true)
              when(mockService.isVariablePaymentPlan(any()))
                .thenReturn(false)

              val summaryListRows = summaryList(mockSinglePaymentPlanDetailResponse, application)
              val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
              val result = route(application, request).value
              val view = application.injector.instanceOf[PaymentPlanDetailsView]

              val formattedTotalAmount = ""
              val formattedDueDate = ""
              val mockPaymentReference = mockSinglePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                "singlePaymentPlan",
                mockPaymentReference,
                true,
                true,
                false,
                false,
                "",
                "",
                summaryListRows,
                false,
                formattedTotalAmount,
                formattedDueDate,
                true,
                routes.AdvanceNoticeController.onPageLoad()
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "should not show Amend, Cancel Suspend actions when scheduledPaymentStartDate is within 2 working days" in {
            val mockSinglePaymentPlanDetailResponse =
              dummyPlanDetailResponse.copy(paymentPlanDetails =
                dummyPlanDetailResponse.paymentPlanDetails.copy(
                  planType                  = PaymentPlanType.SinglePaymentPlan.toString,
                  scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                  scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                  suspensionStartDate       = None,
                  suspensionEndDate         = None,
                  paymentPlanEditable       = true
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
                .thenReturn(Future.successful(mockSinglePaymentPlanDetailResponse))
              when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                .thenReturn(Future.successful(false))
              when(mockService.isPaymentPlanEditable(any()))
                .thenReturn(true)
              when(mockService.isVariablePaymentPlan(any()))
                .thenReturn(false)

              val summaryListRows = summaryList(mockSinglePaymentPlanDetailResponse, application)
              val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
              val result = route(application, request).value
              val view = application.injector.instanceOf[PaymentPlanDetailsView]
              val mockPaymentReference = mockSinglePaymentPlanDetailResponse.paymentPlanDetails.paymentReference
              status(result) mustEqual OK
              contentAsString(result) mustEqual view("singlePaymentPlan",
                                                     mockPaymentReference,
                                                     false,
                                                     false,
                                                     false,
                                                     false,
                                                     "",
                                                     "",
                                                     summaryListRows,
                                                     false,
                                                     "",
                                                     "",
                                                     false,
                                                     routes.AdvanceNoticeController.onPageLoad()
                                                    )(
                request,
                messages(application)
              ).toString
            }
          }
        }
      }

      ".BudgetPayment Plan" - {

        def summaryListWithEndDateAndWithoutSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            ),
            AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app))
          )
        }

        def summaryListWithoutEndDateAndWithoutSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            )
          )
        }

        def summaryListWithEndDateAndWithSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            ),
            AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app)),
            SuspensionPeriodRangeDateSummary.row(planDetail.suspensionStartDate, planDetail.suspensionEndDate, true)(messages(app))
          )
        }

        def summaryListWithoutEndDateAndWithSuspendPeriod(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
            PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            ),
            SuspensionPeriodRangeDateSummary.row(planDetail.suspensionStartDate, planDetail.suspensionEndDate, true)(messages(app))
          )
        }

        "must return OK and the correct view for a GET" - {

          "when Suspension is inactive" - {
            "should show Amend, Cancel and Suspend actions when payment plan is not started" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  true,
                  true,
                  true,
                  false,
                  "",
                  "",
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should show Amend, Cancel and Suspend actions when scheduledPaymentEndDate is None" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    scheduledPaymentEndDate   = None,
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithoutEndDateAndWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  true,
                  true,
                  true,
                  false,
                  "",
                  "",
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should show Amend, Cancel and Suspend actions when scheduledPaymentStartDate is a past date" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(30).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan",
                                                       mockPaymentReference,
                                                       true,
                                                       true,
                                                       true,
                                                       false,
                                                       "",
                                                       "",
                                                       summaryListRows,
                                                       false,
                                                       "",
                                                       "",
                                                       true,
                                                       routes.AdvanceNoticeController.onPageLoad()
                                                      )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show Amend, Cancel and Suspend actions when scheduledPaymentEndDate is within 3 working days" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(3).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(3).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
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
                  .thenReturn(Future.successful(false))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  false,
                  false,
                  false,
                  false,
                  "",
                  "",
                  summaryListRows,
                  false,
                  "",
                  "",
                  false,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show Amend, Cancel and Suspend actions when payment plan is inactive" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(30).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().minusDays(5).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithoutSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view("budgetPaymentPlan",
                                                       mockPaymentReference,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       "",
                                                       "",
                                                       summaryListRows,
                                                       false,
                                                       "",
                                                       "",
                                                       false,
                                                       routes.AdvanceNoticeController.onPageLoad()
                                                      )(
                  request,
                  messages(application)
                ).toString
              }
            }
          }

          "when Suspension is active" - {
            "should show Amend and Cancel actions but not Suspend action when payment plan is not started" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(30).toLocalDate),
                    suspensionStartDate       = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(15).toLocalDate),
                    paymentPlanEditable       = true
                  )
                )

              val formattedSuspensionStartDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionStartDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

              val formattedSuspensionEndDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionEndDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  true,
                  true,
                  false,
                  true,
                  formattedSuspensionStartDate,
                  formattedSuspensionEndDate,
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should show Amend and Cancel actions but not Suspend action when scheduledPaymentEndDate is None" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    scheduledPaymentEndDate   = None,
                    suspensionStartDate       = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(15).toLocalDate),
                    paymentPlanEditable       = true
                  )
                )

              val formattedSuspensionStartDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionStartDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

              val formattedSuspensionEndDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionEndDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

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
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithoutEndDateAndWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  true,
                  true,
                  false,
                  true,
                  formattedSuspensionStartDate,
                  formattedSuspensionEndDate,
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should show Amend and Cancel actions but not Suspend action when scheduledPaymentStartDate is past date but should show the suspend period" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().minusDays(5).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(30).toLocalDate),
                    suspensionStartDate       = Some(LocalDateTime.now().plusDays(5).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                    paymentPlanEditable       = true
                  )
                )

              val formattedSuspensionStartDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionStartDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

              val formattedSuspensionEndDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionEndDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  true,
                  true,
                  false,
                  true,
                  formattedSuspensionStartDate,
                  formattedSuspensionEndDate,
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }

            "should not show Amend, Cancel and Suspend actions when scheduledPaymentEndDate is within 3 working days but should show the suspend period" in {
              val mockBudgetPaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(1).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(3).toLocalDate),
                    suspensionStartDate       = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                    suspensionEndDate         = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                    paymentPlanEditable       = true
                  )
                )

              val formattedSuspensionStartDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionStartDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

              val formattedSuspensionEndDate = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.suspensionEndDate
                .map(_.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)))
                .getOrElse("")

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
                when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
                  .thenReturn(Future.successful(false))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(false)

                val summaryListRows = summaryListWithEndDateAndWithSuspendPeriod(mockBudgetPaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "budgetPaymentPlan",
                  mockPaymentReference,
                  false,
                  false,
                  false,
                  true,
                  formattedSuspensionStartDate,
                  formattedSuspensionEndDate,
                  summaryListRows,
                  false,
                  "",
                  "",
                  false,
                  routes.AdvanceNoticeController.onPageLoad()
                )(
                  request,
                  messages(application)
                ).toString
              }
            }
          }
        }
      }

      ".VariablePayment Plan" - {

        def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
          val planDetail = paymentPlanData.paymentPlanDetails
          Seq(
            AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
            AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
            DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
            AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate, Constants.shortDateTimeFormatPattern)(
              messages(app)
            )
          )
        }

        "must return OK and the correct view for a GET" - {

          "should show Cancel action but not Amend and Suspend actions when scheduledPaymentStartDate is beyond 2 working days" in {
            val mockVariablePaymentPlanDetailResponse =
              dummyPlanDetailResponse.copy(paymentPlanDetails =
                dummyPlanDetailResponse.paymentPlanDetails.copy(
                  planType                  = PaymentPlanType.VariablePaymentPlan.toString,
                  scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(1).toLocalDate),
                  scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(10).toLocalDate),
                  suspensionStartDate       = None,
                  suspensionEndDate         = None,
                  paymentPlanEditable       = true
                )
              )

            val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
              totalAmount = None,
              dueDate     = None
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
              when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                .thenReturn(Future.successful(true))
              when(mockService.isPaymentPlanEditable(any()))
                .thenReturn(true)
              when(mockService.isVariablePaymentPlan(any()))
                .thenReturn(true)
              when(mockService.isAdvanceNoticePresent(any(), any())(any()))
                .thenReturn(Future.successful(advanceNoticeResponse))

              val summaryListRows = summaryList(mockVariablePaymentPlanDetailResponse, application)
              val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
              val result = route(application, request).value
              val view = application.injector.instanceOf[PaymentPlanDetailsView]
              val mockPaymentReference = mockVariablePaymentPlanDetailResponse.paymentPlanDetails.paymentReference
              status(result) mustEqual OK
              contentAsString(result) mustEqual view("variablePaymentPlan",
                                                     mockPaymentReference,
                                                     false,
                                                     true,
                                                     false,
                                                     false,
                                                     "",
                                                     "",
                                                     summaryListRows,
                                                     false,
                                                     "",
                                                     "",
                                                     true,
                                                     routes.AdvanceNoticeController.onPageLoad()
                                                    )(
                request,
                messages(application)
              ).toString
            }
          }

          "should not show Amend, Cancel and Suspend actions when scheduledPaymentStartDate is within 2 working days" in {
            val mockVariablePaymentPlanDetailResponse =
              dummyPlanDetailResponse.copy(paymentPlanDetails =
                dummyPlanDetailResponse.paymentPlanDetails.copy(
                  planType                  = PaymentPlanType.VariablePaymentPlan.toString,
                  scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                  scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                  suspensionStartDate       = None,
                  suspensionEndDate         = None,
                  paymentPlanEditable       = true
                )
              )

            val paymentPlanReference = "ppReference"
            val directDebitReference = "ddReference"

            val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
              totalAmount = None,
              dueDate     = None
            )

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
              when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                .thenReturn(Future.successful(false))
              when(mockService.isPaymentPlanEditable(any()))
                .thenReturn(true)
              when(mockService.isVariablePaymentPlan(any()))
                .thenReturn(true)
              when(mockService.isAdvanceNoticePresent(any(), any())(any()))
                .thenReturn(Future.successful(advanceNoticeResponse))

              val summaryListRows = summaryList(mockVariablePaymentPlanDetailResponse, application)
              val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
              val result = route(application, request).value
              val view = application.injector.instanceOf[PaymentPlanDetailsView]
              val mockPaymentReference = mockVariablePaymentPlanDetailResponse.paymentPlanDetails.paymentReference
              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                "variablePaymentPlan",
                mockPaymentReference,
                false,
                false,
                false,
                false,
                "",
                "",
                summaryListRows,
                false,
                "",
                "",
                false,
                routes.AdvanceNoticeController.onPageLoad()
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "Advance Notice Details Present" - {
            "should show advance notice details when present" in {
              val mockVariablePaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.VariablePaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(4).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
                totalAmount = Some(BigDecimal(500)),
                dueDate     = Some(LocalDate.now().plusMonths(1))
              )

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
                  .set(AdvanceNoticeResponseQuery, advanceNoticeResponse)
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
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(true)
                when(mockService.isAdvanceNoticePresent(any(), any())(any()))
                  .thenReturn(Future.successful(advanceNoticeResponse))

                val summaryListRows = summaryList(mockVariablePaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]

                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
                val formattedTotalAmount = advanceNoticeResponse.totalAmount
                  .map(amount => currencyFormat.format(amount.bigDecimal))
                  .getOrElse("£0.00")

                val formattedDueDate = advanceNoticeResponse.dueDate
                  .map(_.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
                  .getOrElse("")

                val mockPaymentReference = mockVariablePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "variablePaymentPlan",
                  mockPaymentReference,
                  false,
                  true,
                  false,
                  false,
                  "",
                  "",
                  summaryListRows,
                  true,
                  formattedTotalAmount,
                  formattedDueDate,
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(request, messages(application)).toString
              }
            }
          }
          "Advance Notice Details Not Present" - {
            "should not show advance notice details when not present" in {
              val mockVariablePaymentPlanDetailResponse =
                dummyPlanDetailResponse.copy(paymentPlanDetails =
                  dummyPlanDetailResponse.paymentPlanDetails.copy(
                    planType                  = PaymentPlanType.VariablePaymentPlan.toString,
                    scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(4).toLocalDate),
                    scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                    suspensionStartDate       = None,
                    suspensionEndDate         = None,
                    paymentPlanEditable       = true
                  )
                )

              val paymentPlanReference = "ppReference"
              val directDebitReference = "ddReference"

              val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
                totalAmount = None,
                dueDate     = None
              )

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
                when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
                  .thenReturn(Future.successful(true))
                when(mockService.isPaymentPlanEditable(any()))
                  .thenReturn(true)
                when(mockService.isVariablePaymentPlan(any()))
                  .thenReturn(true)
                when(mockService.isAdvanceNoticePresent(any(), any())(any()))
                  .thenReturn(Future.successful(advanceNoticeResponse))

                val summaryListRows = summaryList(mockVariablePaymentPlanDetailResponse, application)
                val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
                val result = route(application, request).value
                val view = application.injector.instanceOf[PaymentPlanDetailsView]
                val mockPaymentReference = mockVariablePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

                status(result) mustEqual OK
                contentAsString(result) mustEqual view(
                  "variablePaymentPlan",
                  mockPaymentReference,
                  false,
                  true,
                  false,
                  false,
                  "",
                  "",
                  summaryListRows,
                  false,
                  "",
                  "",
                  true,
                  routes.AdvanceNoticeController.onPageLoad()
                )(request, messages(application)).toString
              }
            }

          }
        }
      }

      ".TaxCreditRepayment Plan" - {

        def varRepaySummaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
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
            AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate, Constants.shortDateTimeFormatPattern)(messages(app))
          )
        }

        val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
          totalAmount = None,
          dueDate     = None
        )

        "must return OK and the correct view for a GET" - {
          "should not show Amend, Cancel and Suspend actions" in {
            val mockTaxCreditRepaymentPlanDetailResponse =
              dummyPlanDetailResponse.copy(paymentPlanDetails =
                dummyPlanDetailResponse.paymentPlanDetails.copy(
                  planType                  = PaymentPlanType.TaxCreditRepaymentPlan.toString,
                  scheduledPaymentStartDate = Some(LocalDateTime.now().plusDays(2).toLocalDate),
                  scheduledPaymentEndDate   = Some(LocalDateTime.now().plusDays(20).toLocalDate),
                  suspensionStartDate       = None,
                  suspensionEndDate         = None,
                  paymentPlanEditable       = true
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
                .thenReturn(Future.successful(mockTaxCreditRepaymentPlanDetailResponse))
              when(mockService.isPaymentPlanEditable(any()))
                .thenReturn(true)
              when(mockService.isVariablePaymentPlan(any()))
                .thenReturn(false)
              when(mockService.isAdvanceNoticePresent(any(), any())(any()))
                .thenReturn(Future.successful(advanceNoticeResponse))

              val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
              val summaryListRows = varRepaySummaryList(mockTaxCreditRepaymentPlanDetailResponse, application)
              val result = route(application, request).value
              val view = application.injector.instanceOf[PaymentPlanDetailsView]
              val mockPaymentReference = mockTaxCreditRepaymentPlanDetailResponse.paymentPlanDetails.paymentReference
              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                "taxCreditRepaymentPlan",
                mockPaymentReference,
                false,
                false,
                false,
                false,
                "",
                "",
                summaryListRows,
                false,
                "",
                "",
                false,
                routes.AdvanceNoticeController.onPageLoad()
              )(
                request,
                messages(application)
              ).toString
            }
          }
        }
      }

      "must redirect to Journey Recovery page when DirectDebitReferenceQuery is not set" in {
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

      "should redirect to PaymentPlanLockedWarningController when payment plan is locked" in {
        val mockPaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(
              planType            = PaymentPlanType.SinglePaymentPlan.toString,
              paymentPlanEditable = false
            )
          )

        val paymentPlanReference = "ppReference"
        val directDebitReference = "ddReference"

        val userAnswersWithPaymentReference =
          emptyUserAnswers
            .set(PaymentPlanReferenceQuery, paymentPlanReference)
            .success
            .value
            .set(DirectDebitReferenceQuery, directDebitReference)
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
            .thenReturn(Future.successful(mockPaymentPlanDetailResponse))
          when(mockService.isPaymentPlanEditable(any()))
            .thenReturn(false)

          val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PaymentPlanLockedWarningController.onPageLoad().url
        }
      }

      "must remove RemovingThisSuspensionPage from session when page loads" in {
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
            .set(RemovingThisSuspensionPage, true)
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
          when(mockService.isPaymentPlanEditable(any()))
            .thenReturn(true)

          val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK

          val captor = ArgumentCaptor.forClass(classOf[models.UserAnswers])
          verify(mockSessionRepository, atLeastOnce()).set(captor.capture())

          val capturedList = captor.getAllValues
          val finalSavedAnswers = capturedList.get(capturedList.size() - 1)
          finalSavedAnswers.get(RemovingThisSuspensionPage) mustBe None
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
