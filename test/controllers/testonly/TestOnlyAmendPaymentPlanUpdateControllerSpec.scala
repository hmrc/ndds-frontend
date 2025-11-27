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
import config.FrontendAppConfig
import models.PaymentPlanType
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{AmendPaymentAmountPage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import views.html.testonly.TestOnlyAmendPaymentPlanUpdateView

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.Locale

class TestOnlyAmendPaymentPlanUpdateControllerSpec extends SpecBase {

  "PaymentPlanConfirmation Controller" - {
    val mockService = mock[NationalDirectDebitService]
    val regPaymentAmount: BigDecimal = BigDecimal("1000.00")
    val startDate: LocalDate = LocalDate.of(2025, 10, 2)

    "must return OK and the correct view for a GET when plan type is Single Payment Plan" in {

      val mockSinglePaymentPlanDetailResponse =
        dummyPlanDetailResponse.copy(
          paymentPlanDetails = dummyPlanDetailResponse.paymentPlanDetails.copy(
            planType = PaymentPlanType.SinglePaymentPlan.toString
          )
        )

      val directDebitRef = "DD-REF-123"
      val paymentPlanRef = "PP-REF-987"

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
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value
        .set(DirectDebitReferenceQuery, directDebitRef)
        .success
        .value
        .set(PaymentPlanReferenceQuery, paymentPlanRef)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(inject.bind[NationalDirectDebitService].toInstance(mockService))
        .build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateController]
        val request = FakeRequest(GET, routes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url)
        val result = controller.onPageLoad()(request)

        val view = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
        val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")
        val dateFormatMonth = DateTimeFormatter.ofPattern("d MMM yyyy")

        val formattedRegPaymentAmount = currencyFormat.format(regPaymentAmount)
        val formattedStartDate = dateFormat.format(startDate)
        val formattedStartDateMonth = dateFormatMonth.format(startDate)
        val formattedSubmissionDate =
          dateFormatMonth.format(mockSinglePaymentPlanDetailResponse.paymentPlanDetails.submissionDateTime)

        val dd = mockSinglePaymentPlanDetailResponse.directDebitDetails
        val formattedSortCode = dd.bankSortCode
          .map(sc => sc.grouped(2).mkString(" "))
          .getOrElse("")
        val paymentReference = mockSinglePaymentPlanDetailResponse.paymentPlanDetails.paymentReference

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          appConfig.hmrcHelplineUrl,
          formattedRegPaymentAmount,
          formattedStartDate,
          directDebitRef,
          dd.bankAccountName.getOrElse(""),
          dd.bankAccountNumber.getOrElse(""),
          formattedSortCode,
          paymentReference,
          formattedSubmissionDate,
          formattedStartDateMonth,
          controllers.routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recover page when AmendPlanStartDatePage is None" in {

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
        .set(AmendPaymentAmountPage, regPaymentAmount)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateController]
        val request = FakeRequest(GET, routes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url)
        val result = controller.onPageLoad()(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recover page when AmendPaymentAmountPage is None" in {

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
        .set(AmendPlanStartDatePage, startDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)

        val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateController]
        val request = FakeRequest(GET, routes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url)
        val result = controller.onPageLoad()(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery page when PaymentPlanDetailsQuery is None" in {
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

        val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateController]
        val request = FakeRequest(GET, routes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url)
        val result = controller.onPageLoad()(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery page when amend payment plan guard returns false" in {
      val userAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)

        val controller = application.injector.instanceOf[TestOnlyAmendPaymentPlanUpdateController]
        val request = FakeRequest(GET, routes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad().url)
        val result = controller.onPageLoad()(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

  }

}
