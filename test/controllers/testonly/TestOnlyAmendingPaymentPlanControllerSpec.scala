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
import models.PaymentPlanType
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.ManagePaymentPlanTypePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService

import java.time.{LocalDate, LocalDateTime}

class TestOnlyAmendingPaymentPlanControllerSpec extends SpecBase {

  "TestOnlyAmendingPaymentPlanController" - {
    val mockService = mock[NationalDirectDebitService]

    def makePlanDetails(
      planType: PaymentPlanType,
      amount: BigDecimal,
      startDate: LocalDate,
      endDate: Option[LocalDate]
    ): PaymentPlanDetails =
      PaymentPlanDetails(
        hodService                = "HOD",
        planType                  = planType.toString,
        paymentReference          = "REF123",
        submissionDateTime        = LocalDateTime.now(),
        scheduledPaymentAmount    = Some(amount),
        scheduledPaymentStartDate = Some(startDate),
        initialPaymentStartDate   = None,
        initialPaymentAmount      = None,
        scheduledPaymentEndDate   = endDate,
        scheduledPaymentFrequency = None,
        suspensionStartDate       = None,
        suspensionEndDate         = None,
        balancingPaymentAmount    = None,
        balancingPaymentDate      = None,
        totalLiability            = None,
        paymentPlanEditable       = true
      )

    def commonDdDetails: DirectDebitDetails =
      DirectDebitDetails(
        bankSortCode       = None,
        bankAccountNumber  = None,
        bankAccountName    = None,
        auDdisFlag         = false,
        submissionDateTime = LocalDateTime.now()
      )

    "must return OK and render correct view for GET (Single Payment)" in {
      val planDetails =
        makePlanDetails(
          planType  = PaymentPlanType.SinglePaymentPlan,
          amount    = 100,
          startDate = LocalDate.of(2025, 11, 25),
          endDate   = None
        )

      val ddDetails = commonDdDetails
      val wrapped = PaymentPlanResponse(ddDetails, planDetails)
      val ua = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, wrapped)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(Some(ua))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.TestOnlyAmendingPaymentPlanController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        val page = contentAsString(result)

        page must include("Amending this payment plan")
        page must include("For security reasons")
        page must include("£100.00")
        page must include("25 Nov 2025")
      }
    }

    "must redirect to Journey Recovery page when amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val controller = application.injector.instanceOf[TestOnlyAmendingPaymentPlanController]
        val request = FakeRequest(GET, routes.TestOnlyAmendingPaymentPlanController.onPageLoad().url)
        val result = controller.onPageLoad()(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must show end-date row when BudgetPaymentPlan HAS an end date" in {
      val end = LocalDate.of(2026, 3, 15)
      val planDetails =
        makePlanDetails(
          planType  = PaymentPlanType.BudgetPaymentPlan,
          amount    = 50,
          startDate = LocalDate.of(2025, 1, 1),
          endDate   = Some(end)
        )
      val wrapped = PaymentPlanResponse(commonDdDetails, planDetails)
      val ua = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, wrapped)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(Some(ua))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.TestOnlyAmendingPaymentPlanController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val page = contentAsString(result)
        page must include("Amending this payment plan")
        page must include("15 Mar 2026")
      }
    }

    "must show 'Add end date' link when BudgetPaymentPlan has NO end date" in {
      val planDetails =
        makePlanDetails(
          planType  = PaymentPlanType.BudgetPaymentPlan,
          amount    = 50,
          startDate = LocalDate.of(2025, 1, 1),
          endDate   = None
        )
      val wrapped = PaymentPlanResponse(commonDdDetails, planDetails)
      val ua = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, wrapped)
        .success
        .value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
      val application = applicationBuilder(Some(ua))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()
      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val request = FakeRequest(GET, routes.TestOnlyAmendingPaymentPlanController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val page = contentAsString(result)
        page must include("Amending this payment plan")
        page must include("Add plan end date")
        page must include("/direct-debits/date-ending-payment-plan")
      }
    }
  }
}
