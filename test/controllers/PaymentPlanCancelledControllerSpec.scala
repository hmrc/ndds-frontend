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
import models.responses.PaymentPlanDetails
import org.scalatestplus.mockito.MockitoSugar
import pages.ManagePaymentPlanTypePage
import play.api.Application
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.PaymentPlanCancelledView

import java.time.LocalDate

class PaymentPlanCancelledControllerSpec extends SpecBase with MockitoSugar {

  private lazy val paymentPlanCancelledRoute = routes.PaymentPlanCancelledController.onPageLoad().url

  "PaymentPlanCancelled Controller" - {

    "must return OK and the correct view for a GET with BudgetPaymentPlan" in {

      def summaryList(paymentPlanDetails: PaymentPlanDetails, app: Application): Seq[SummaryListRow] =
        Seq(
          AmendPaymentPlanTypeSummary.row(paymentPlanDetails.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(paymentPlanDetails.hodService)(messages(app)),
          DateSetupSummary.row(paymentPlanDetails.submissionDateTime)(messages(app)),
          AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)(messages(app)),
          PaymentsFrequencySummary.row(paymentPlanDetails.scheduledPaymentFrequency)(messages(app)),
          AmendPlanStartDateSummary.row(paymentPlanDetails.planType,
                                        paymentPlanDetails.scheduledPaymentStartDate,
                                        Constants.shortDateTimeFormatPattern
                                       )(messages(app))
        ) ++
          paymentPlanDetails.scheduledPaymentEndDate.fold(Seq.empty[SummaryListRow]) { endDate =>
            Seq(AmendPlanEndDateSummary.row(Some(endDate), Constants.shortDateTimeFormatPattern)(messages(app)))
          } ++
          (if (paymentPlanDetails.suspensionEndDate.exists(!LocalDate.now().isAfter(_))) {
             Seq(
               SuspensionPeriodRangeDateSummary.row(paymentPlanDetails.suspensionStartDate, paymentPlanDetails.suspensionEndDate, showActions = true)(
                 messages(app)
               )
             )
           } else {
             Seq.empty
           })

      val mockBudgetPaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
        )

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData))
        .build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanCancelledRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PaymentPlanCancelledView]
        val summaryListRows = summaryList(userAnswersWithData.get(PaymentPlanDetailsQuery).get.paymentPlanDetails, application)
        val mockPaymentReference = mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockPaymentReference,
                                               Call("GET", routes.YourDirectDebitInstructionsController.onPageLoad().url),
                                               summaryListRows
                                              )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET with SinglePaymentPlan" in {

      def summaryList(paymentPlanDetails: PaymentPlanDetails, app: Application): Seq[SummaryListRow] =
        Seq(
          AmendPaymentPlanTypeSummary.row(paymentPlanDetails.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(paymentPlanDetails.hodService)(messages(app)),
          DateSetupSummary.row(paymentPlanDetails.submissionDateTime)(messages(app)),
          AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)(messages(app)),
          AmendPlanStartDateSummary.row(paymentPlanDetails.planType,
                                        paymentPlanDetails.scheduledPaymentStartDate,
                                        Constants.shortDateTimeFormatPattern
                                       )(messages(app))
        )

      val mockSinglePaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.SinglePaymentPlan.toString)
        )

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanDetailsQuery,
            mockSinglePaymentPlanDetailResponse
          )
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData))
        .build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanCancelledRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PaymentPlanCancelledView]
        val summaryListRows = summaryList(userAnswersWithData.get(PaymentPlanDetailsQuery).get.paymentPlanDetails, application)
        val mockPaymentReference = mockSinglePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockPaymentReference,
                                               Call("GET", routes.YourDirectDebitInstructionsController.onPageLoad().url),
                                               summaryListRows
                                              )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET with VariablePaymentPlan" in {

      def summaryList(paymentPlanDetails: PaymentPlanDetails, app: Application): Seq[SummaryListRow] =
        Seq(
          AmendPaymentPlanTypeSummary.row(paymentPlanDetails.planType)(messages(app)),
          AmendPaymentPlanSourceSummary.row(paymentPlanDetails.hodService)(messages(app)),
          DateSetupSummary.row(paymentPlanDetails.submissionDateTime)(messages(app)),
          AmendPlanStartDateSummary.row(
            paymentPlanDetails.planType,
            paymentPlanDetails.scheduledPaymentStartDate,
            Constants.shortDateTimeFormatPattern
          )(messages(app))
        )

      val mockVariablePaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(paymentPlanDetails =
          dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.VariablePaymentPlan.toString)
        )

      val userAnswersWithData =
        emptyUserAnswers
          .set(
            PaymentPlanDetailsQuery,
            mockVariablePaymentPlanDetailResponse
          )
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData))
        .build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanCancelledRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[PaymentPlanCancelledView]
        val summaryListRows = summaryList(userAnswersWithData.get(PaymentPlanDetailsQuery).get.paymentPlanDetails, application)
        val mockPaymentReference = mockVariablePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockPaymentReference,
                                               Call("GET", routes.YourDirectDebitInstructionsController.onPageLoad().url),
                                               summaryListRows
                                              )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanCancelledRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if empty data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanCancelledRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
