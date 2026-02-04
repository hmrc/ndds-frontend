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
import models.responses.PaymentPlanDetails
import models.{PaymentPlanType, SuspensionPeriodRange, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ManagePaymentPlanTypePage, SuspensionPeriodRangeDatePage}
import play.api.Application
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, PaymentReferenceSummary, SuspensionPeriodRangeDateSummary}
import views.html.PaymentPlanSuspendedView

import java.time.format.DateTimeFormatter

class PaymentPlanSuspendedControllerSpec extends SpecBase with MockitoSugar {

  private lazy val paymentPlanSuspendedRoute =
    routes.PaymentPlanSuspendedController.onPageLoad().url

  private val suspensionRange = SuspensionPeriodRange(
    startDate = java.time.LocalDate.of(2025, 10, 25),
    endDate   = java.time.LocalDate.of(2025, 11, 16)
  )

  private val mockBudgetPaymentPlanDetailResponse =
    dummyPlanDetailResponse.copy(
      paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
        planType = PaymentPlanType.BudgetPaymentPlan.toString
      )
    )

  "PaymentPlanSuspended Controller" - {

    "must return OK and show correct view for GET when UserAnswers contains all required data" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
          .success
          .value
          .set(SuspensionPeriodRangeDatePage, suspensionRange)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value

      val application = applicationBuilder(Some(userAnswersWithData)).build()

      def summaryList(
        paymentReference: String,
        userAnswers: UserAnswers,
        paymentPlanDetails: PaymentPlanDetails,
        app: Application
      ): Seq[SummaryListRow] = {

        Seq(
          Some(PaymentReferenceSummary.row(paymentReference)(messages(app))),
          Some(
            AmendPaymentAmountSummary
              .row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)(messages(app))
          ),
          SuspensionPeriodRangeDateSummary.row(
            userAnswers,
            showChange          = false,
            isSuspendChangeMode = false
          )(messages(app))
        ).flatten
      }

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)
        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanSuspendedView]

        val formattedStartDate =
          suspensionRange.startDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val formattedEndDate =
          suspensionRange.endDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))

        val mockPaymentReference =
          mockBudgetPaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        val summaryListRows =
          summaryList(
            mockPaymentReference,
            userAnswersWithData,
            mockBudgetPaymentPlanDetailResponse.paymentPlanDetails,
            application
          )

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          formattedStartDate,
          formattedEndDate,
          Call("GET", routes.PaymentPlanDetailsController.onPageLoad().url),
          summaryListRows,
          suspensionIsActiveMode = false
        )(request, messages(application)).toString
      }
    }

    "must redirect to System Error when PaymentPlanDetails missing" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(SuspensionPeriodRangeDatePage, suspensionRange)
          .success
          .value

      val application = applicationBuilder(Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error when SuspensionPeriodRangeDate missing" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
          .success
          .value

      val application = applicationBuilder(Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
