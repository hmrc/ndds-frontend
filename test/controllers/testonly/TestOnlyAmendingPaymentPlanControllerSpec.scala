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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery

import java.time.{LocalDate, LocalDateTime}

class TestOnlyAmendingPaymentPlanControllerSpec extends SpecBase {

  "TestOnlyAmendingPaymentPlanController" - {
    "must return OK and render correct view for GET (Single Payment)" in {
      val planDetails = PaymentPlanDetails(
        hodService                = "HOD",
        planType                  = PaymentPlanType.SinglePaymentPlan.toString,
        paymentReference          = "ABC123",
        submissionDateTime        = LocalDateTime.now(),
        scheduledPaymentAmount    = Some(BigDecimal(100)),
        scheduledPaymentStartDate = Some(LocalDate.of(2025, 11, 25)),
        initialPaymentStartDate   = None,
        initialPaymentAmount      = None,
        scheduledPaymentEndDate   = None,
        scheduledPaymentFrequency = None,
        suspensionStartDate       = None,
        suspensionEndDate         = None,
        balancingPaymentAmount    = None,
        balancingPaymentDate      = None,
        totalLiability            = None,
        paymentPlanEditable       = true
      )

      val ddDetails = DirectDebitDetails(
        bankSortCode       = None,
        bankAccountNumber  = None,
        bankAccountName    = None,
        auDdisFlag         = false,
        submissionDateTime = LocalDateTime.now()
      )

      val wrapped = PaymentPlanResponse(
        directDebitDetails = ddDetails,
        paymentPlanDetails = planDetails
      )

      val ua = emptyUserAnswers
        .set(PaymentPlanDetailsQuery, wrapped)
        .success
        .value

      val application = applicationBuilder(Some(ua))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
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
  }
}
