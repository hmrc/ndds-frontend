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
import models.responses.PaymentPlanDetails
import models.{PaymentPlanType, SuspensionPeriodRange, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar
import pages.SuspensionPeriodRangeDatePage
import play.api.Application
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, PaymentReferenceSummary, SuspensionPeriodRangeDateSummary}
import views.html.PaymentPlanSuspendedView

import java.time.format.DateTimeFormatter

class PaymentPlanSuspendedControllerSpec extends SpecBase with MockitoSugar {

  private lazy val paymentPlanSuspendedRoute = routes.PaymentPlanSuspendedController.onPageLoad().url

  private val suspensionRange = SuspensionPeriodRange(
    startDate = java.time.LocalDate.of(2025, 10, 25),
    endDate   = java.time.LocalDate.of(2025, 11, 16)
  )

  private val paymentPlanReference = "ppReference"

  private val mockBudgetPaymentPlanDetailResponse =
    dummyPlanDetailResponse.copy(paymentPlanDetails =
      dummyPlanDetailResponse.paymentPlanDetails.copy(planType = PaymentPlanType.BudgetPaymentPlan.toString)
    )

  "PaymentPlanSuspended Controller" - {

    "must return OK and the correct view for a GET when UserAnswers has all required data" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value
          .set(SuspensionPeriodRangeDatePage, suspensionRange)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      def summaryList(paymentPlanReference: String,
                      userAnswers: UserAnswers,
                      paymentPlanDetails: PaymentPlanDetails,
                      app: Application
                     ): Seq[SummaryListRow] =
        Seq(
          Some(PaymentReferenceSummary.row(paymentPlanReference)(messages(app))),
          Some(AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)(messages(app))),
          SuspensionPeriodRangeDateSummary.row(userAnswers)(messages(app))
        ).flatten

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanSuspendedView]

        val formattedStartDate = userAnswersWithData
          .get(SuspensionPeriodRangeDatePage)
          .get
          .startDate
          .format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val formattedEndDate =
          userAnswersWithData.get(SuspensionPeriodRangeDatePage).get.endDate.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))

        val summaryListRows =
          summaryList(paymentPlanReference, userAnswersWithData, userAnswersWithData.get(PaymentPlanDetailsQuery).get.paymentPlanDetails, application)

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(paymentPlanReference,
                                               formattedStartDate,
                                               formattedEndDate,
                                               Call("GET", routes.PaymentPlanDetailsController.onPageLoad().url),
                                               summaryListRows
                                              )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return SEE_OTHER for a GET when UserAnswers is missing PaymentPlanReference" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value
          .set(SuspensionPeriodRangeDatePage, suspensionRange)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return SEE_OTHER for a GET when UserAnswers is missing PaymentPlanDetails" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(SuspensionPeriodRangeDatePage, suspensionRange)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return SEE_OTHER for a GET when UserAnswers is missing SuspensionPeriodRangeDate" in {

      val userAnswersWithData: UserAnswers =
        emptyUserAnswers
          .set(
            PaymentPlanReferenceQuery,
            paymentPlanReference
          )
          .success
          .value
          .set(
            PaymentPlanDetailsQuery,
            mockBudgetPaymentPlanDetailResponse
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, paymentPlanSuspendedRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

  }
}
