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
import models.{PaymentPlanType, UserAnswers}
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, PaymentReferenceSummary}
import views.html.AmendPaymentPlanUpdateView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AmendPaymentPlanUpdateControllerSpec extends SpecBase with MockitoSugar {

  "PaymentPlanConfirmation Controller" - {
    val mockService = mock[NationalDirectDebitService]
    val regPaymentAmount: BigDecimal = BigDecimal("1000.00")
    val formattedRegPaymentAmount: String = formatAmount(regPaymentAmount)
    val startDate: LocalDate = LocalDate.of(2025, 10, 2)
    val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    val endDate: LocalDate = LocalDate.of(2025, 10, 25)

    "must return OK and the correct view for a GET when plan type is Budget Payment Plan" in {

      def summaryList(userAnswers: UserAnswers, paymentReference: String, app: Application): Seq[SummaryListRow] = {
        val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
        val planStartDate = userAnswers.get(AmendPlanStartDatePage)
        val planEndDate = userAnswers.get(AmendPlanEndDatePage)

        Seq(
          PaymentReferenceSummary.row(paymentReference)(messages(app)),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern)(
            messages(app)
          ),
          AmendPlanEndDateSummary.row(planEndDate, Constants.longDateTimeFormatPattern)(messages(app))
        )
      }

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
        .success
        .value
        .set(AmendPaymentAmountPage, regPaymentAmount)
        .success
        .value
        .set(AmendPlanStartDatePage, startDate)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendPaymentPlanUpdateView]

        val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
        val summaryListRows = summaryList(userAnswers, mockPaymentReference, application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockPaymentReference, formattedRegPaymentAmount, formattedStartDate, summaryListRows)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when plan type is Single Payment Plan" in {

      def summaryList(userAnswers: UserAnswers, paymentReference: String, app: Application): Seq[SummaryListRow] = {
        val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
        val planStartDate = userAnswers.get(AmendPlanStartDatePage)

        Seq(
          PaymentReferenceSummary.row(paymentReference)(messages(app)),
          AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, paymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(PaymentPlanType.SinglePaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern)(messages(app))
        )
      }

      val mockSinglePaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
        )

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, mockSinglePaymentPlanDetailResponse)
        .success
        .value
        .set(AmendPaymentAmountPage, regPaymentAmount)
        .success
        .value
        .set(AmendPlanStartDatePage, startDate)
        .success
        .value
        .set(AmendPlanEndDatePage, endDate)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendPaymentPlanUpdateView]

        val mockPaymentReference = mockSinglePaymentPlanDetailResponse.paymentPlanDetails.paymentReference
        val summaryListRows = summaryList(userAnswers, mockPaymentReference, application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockPaymentReference, formattedRegPaymentAmount, formattedStartDate, summaryListRows)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recover page when AmendPlanStartDatePage is None" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
        .success
        .value
        .set(AmendPaymentAmountPage, regPaymentAmount)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recover page when AmendPaymentAmountPage is None" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
        .success
        .value
        .set(AmendPlanStartDatePage, startDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recover page when PaymentPlanDetailsQuery is None" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value
        .set(AmendPaymentAmountPage, regPaymentAmount)
        .success
        .value
        .set(AmendPlanStartDatePage, startDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recover page when amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val request = FakeRequest(GET, routes.AmendPaymentPlanUpdateController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
