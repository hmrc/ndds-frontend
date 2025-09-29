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
import models.responses.PaymentPlanResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.PaymentPlanData
import viewmodels.checkAnswers.*
import views.html.PaymentPlanDetailsView

import scala.concurrent.Future

class PaymentPlanDetailsControllerSpec extends SpecBase with PaymentPlanData {

  "PaymentPlanDetails Controller" - {
    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "must return OK and the correct view for a GET with a SinglePayment Plan" in {
      def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
        val planDetail = paymentPlanData.paymentPlanDetails
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
          DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate)(messages(app)),
        )
      }

      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
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
        when(mockService.getPaymentPlanDetails(any()))
          .thenReturn(Future.successful(mockSinglePaymentPlanDetailResponse))
        when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
          .thenReturn(Future.successful(true))

        val summaryListRows = summaryList(mockSinglePaymentPlanDetailResponse, application)

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanDetailsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view("singlePaymentPlan", paymentReference, true, summaryListRows)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with a BudgetPayment Plan" in {
      def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
        val planDetail = paymentPlanData.paymentPlanDetails
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
          DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
          TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
          MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount)(messages(app)),
          FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate)(messages(app)),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate)(messages(app)),
          PaymentsFrequencySummary.row(planDetail.scheduledPaymentFrequency)(messages(app)),
          AmendPaymentAmountSummary.row(planDetail.planType, planDetail.scheduledPaymentAmount)(messages(app)),
          AmendSuspendDateSummary.row(planDetail.suspensionStartDate, true)(messages(app)),
          AmendSuspendDateSummary.row(planDetail.suspensionEndDate, false)(messages(app)),
        )
      }
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
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
        when(mockService.getPaymentPlanDetails(any()))
          .thenReturn(Future.successful(mockBudgetPaymentPlanDetailResponse))
        when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
          .thenReturn(Future.successful(true))
        when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
          .thenReturn(Future.successful(true))

        val summaryListRows = summaryList(mockBudgetPaymentPlanDetailResponse, application)

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanDetailsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view("budgetPaymentPlan", paymentReference, true, summaryListRows)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with a Variable Plan" in {
      def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
        val planDetail = paymentPlanData.paymentPlanDetails
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
          DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
          TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
          MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount)(messages(app)),
          FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate)(messages(app)),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate)(messages(app)),
        )
      }
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
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
        when(mockService.getPaymentPlanDetails(any()))
          .thenReturn(Future.successful(mockVariablePaymentPlanDetailResponse))

        val summaryListRows = summaryList(mockVariablePaymentPlanDetailResponse, application)

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanDetailsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view("variablePaymentPlan", paymentReference, true, summaryListRows)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with a Tax Credit Repayment Plan" in {
      def summaryList(paymentPlanData: PaymentPlanResponse, app: Application): Seq[SummaryListRow] = {
        val planDetail = paymentPlanData.paymentPlanDetails
        Seq(
          AmendPaymentPlanTypeSummary.row(planDetail.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(planDetail.hodService)(messages(app)),
          DateSetupSummary.row(planDetail.submissionDateTime)(messages(app)),
          TotalAmountDueSummary.row(planDetail.totalLiability)(messages(app)),
          MonthlyPaymentAmountSummary.row(planDetail.scheduledPaymentAmount)(messages(app)),
          FinalPaymentAmountSummary.row(planDetail.balancingPaymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(planDetail.planType, planDetail.scheduledPaymentStartDate)(messages(app)),
          AmendPlanEndDateSummary.row(planDetail.scheduledPaymentEndDate)(messages(app))
        )
      }
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
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
        when(mockService.getPaymentPlanDetails(any()))
          .thenReturn(Future.successful(mockTaxCreditRepaymentPlanDetailResponse))

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val summaryListRows = summaryList(mockTaxCreditRepaymentPlanDetailResponse, application)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanDetailsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view("taxCreditRepaymentPlan", paymentReference, false, summaryListRows)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recover page when DirectDebitReferenceQuery is not set" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides().build()

      running(application) {

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Payment Plan Details page when a PaymentReferenceQuery is provided" in {
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
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
