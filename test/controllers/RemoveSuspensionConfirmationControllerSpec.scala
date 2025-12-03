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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
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
import viewmodels.checkAnswers.*
import views.html.RemoveSuspensionConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RemoveSuspensionConfirmationControllerSpec extends SpecBase {

  private lazy val removeSuspensionConfirmationRoute = routes.RemoveSuspensionConfirmationController.onPageLoad().url

  val regPaymentAmount: BigDecimal = BigDecimal("1000.00")
  val formattedRegPaymentAmount: String = formatAmount(regPaymentAmount)
  val startDate: LocalDate = LocalDate.of(2025, 10, 2)
  val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  val endDate: LocalDate = LocalDate.of(2025, 10, 25)

  "RemoveSuspensionConfirmation Controller" - {
    val mockNddsService = mock[NationalDirectDebitService]

    "must return OK and the correct view for a GET with BudgetPaymentPlan with endDate" in {

      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)

      def summaryList(userAnswers: UserAnswers, paymentReference: String, app: Application): Seq[SummaryListRow] = {
        val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
        val planStartDate = userAnswers.get(AmendPlanStartDatePage)
        val planEndDate = userAnswers.get(AmendPlanEndDatePage)

        val baseRows = Seq(
          PaymentReferenceSummary.row(paymentReference)(messages(app)),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, paymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, planStartDate, Constants.longDateTimeFormatPattern)(messages(app))
        )

        planEndDate match {
          case Some(endDate) =>
            baseRows :+ AmendPlanEndDateSummary.row(Some(endDate), Constants.longDateTimeFormatPattern)(messages(app))
          case None => baseRows
        }
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
        .set(AmendPlanEndDatePage, endDate)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[RemoveSuspensionConfirmationView]

        val formattedRegPaymentAmount = formatAmount(regPaymentAmount)
        val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference
        val summaryListRows = summaryList(userAnswers, mockPaymentReference, application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          formattedRegPaymentAmount,
          formattedStartDate,
          summaryListRows,
          routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with BudgetPaymentPlan and no plan end date" in {

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
        // No set for AmendPlanEndDatePage here
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(true)

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[RemoveSuspensionConfirmationView]
        val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        val summaryListRows = Seq(
          PaymentReferenceSummary.row(mockPaymentReference)(messages(application)),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, Some(regPaymentAmount))(messages(application)),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, Some(startDate), Constants.longDateTimeFormatPattern)(
            messages(application)
          )
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          formatAmount(regPaymentAmount),
          startDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)),
          summaryListRows,
          routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecoveryController when suspendPaymentPlanGuard returns false" in {

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddsService))
        .build()

      when(mockNddsService.suspendPaymentPlanGuard(any())).thenReturn(false)

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if empty data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
