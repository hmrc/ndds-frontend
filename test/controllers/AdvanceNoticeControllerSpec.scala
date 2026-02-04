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
import config.FrontendAppConfig
import models.responses.{AdvanceNoticeResponse, DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import pages.ManagePaymentPlanTypePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AdvanceNoticeResponseQuery, DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import views.html.AdvanceNoticeView

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.Locale

class AdvanceNoticeControllerSpec extends SpecBase {

  "AdvanceNotice Controller" - {
    val currentTime = LocalDateTime.now()

    "must return OK and the correct view for a GET" in {
      val directDebitReference = "DD123"
      val paymentPlanReference = "PP456"
      val planType = "04"

      val directDebitDetails: DirectDebitDetails = DirectDebitDetails(
        bankSortCode       = Some("12-34-56"),
        bankAccountNumber  = Some("12345678"),
        bankAccountName    = Some("John Doe"),
        auDdisFlag         = true,
        submissionDateTime = LocalDateTime.now()
      )

      val paymentPlanDetails: PaymentPlanDetails = PaymentPlanDetails(
        hodService                = "HOD1",
        planType                  = planType,
        paymentReference          = paymentPlanReference,
        submissionDateTime        = LocalDateTime.now(),
        scheduledPaymentAmount    = Some(BigDecimal(100)),
        scheduledPaymentStartDate = Some(LocalDate.now()),
        initialPaymentStartDate   = Some(LocalDate.now()),
        initialPaymentAmount      = Some(BigDecimal(50)),
        scheduledPaymentEndDate   = Some(LocalDate.now().plusMonths(6)),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate       = None,
        suspensionEndDate         = None,
        balancingPaymentAmount    = None,
        balancingPaymentDate      = None,
        totalLiability            = Some(BigDecimal(600)),
        paymentPlanEditable       = true
      )

      val paymentPlanResponse: PaymentPlanResponse = PaymentPlanResponse(
        directDebitDetails = directDebitDetails,
        paymentPlanDetails = paymentPlanDetails
      )

      val advanceNoticeResponse: AdvanceNoticeResponse = AdvanceNoticeResponse(
        totalAmount = Some(BigDecimal(500)),
        dueDate     = Some(currentTime.toLocalDate.plusMonths(1))
      )

      val userAnswers = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(DirectDebitReferenceQuery, directDebitReference)
        .success
        .value
        .set(PaymentPlanReferenceQuery, paymentPlanReference)
        .success
        .value
        .set(ManagePaymentPlanTypePage, planType)
        .success
        .value
        .set(AdvanceNoticeResponseQuery, advanceNoticeResponse)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AdvanceNoticeController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[AdvanceNoticeView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
        val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

        val expectedView = view(
          appConfig.hmrcHelplineUrl,
          currencyFormat.format(BigDecimal(500)),
          LocalDate.now().plusMonths(1).format(dateFormat),
          directDebitReference,
          directDebitDetails.bankAccountName.get,
          directDebitDetails.bankAccountNumber.get,
          directDebitDetails.bankSortCode.get,
          paymentPlanReference,
          routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }
  }

}
