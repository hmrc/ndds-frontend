/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import models.PaymentPlanType
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}

import java.time.{LocalDate, LocalDateTime}

trait PaymentPlanData {

  private val now = LocalDateTime.now()

  private val currentDate = LocalDate.now()

  val mockSinglePaymentPlanDetailResponse: PaymentPlanResponse =
    PaymentPlanResponse(
      directDebitDetails = DirectDebitDetails(
        bankSortCode = "123456",
        bankAccountNumber = "12345678",
        bankAccountName = "John Doe",
        auddisFlag = true
      ),
      paymentPlanDetails = PaymentPlanDetails(
        hodService = "NDD",
        planType = PaymentPlanType.SinglePaymentPlan.toString,
        paymentReference = "paymentReference",
        submissionDateTime = now.minusDays(5),
        scheduledPaymentAmount = 120.00,
        scheduledPaymentStartDate = currentDate.plusDays(5),
        initialPaymentStartDate = now,
        initialPaymentAmount = None,
        scheduledPaymentEndDate = currentDate.plusMonths(6),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate = None,
        suspensionEndDate = None,
        balancingPaymentAmount = Some(60.00),
        balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
        totalLiability = Some(780.00),
        paymentPlanEditable = true
      )
    )

  val mockBudgetPaymentPlanDetailResponse: PaymentPlanResponse =
    PaymentPlanResponse(
      directDebitDetails = DirectDebitDetails(
        bankSortCode = "123456",
        bankAccountNumber = "12345678",
        bankAccountName = "John Doe",
        auddisFlag = true
      ),
      paymentPlanDetails = PaymentPlanDetails(
        hodService = "NDD",
        planType = PaymentPlanType.BudgetPaymentPlan.toString,
        paymentReference = "paymentReference",
        submissionDateTime = now.minusDays(5),
        scheduledPaymentAmount = 120.00,
        scheduledPaymentStartDate = currentDate.plusDays(5),
        initialPaymentStartDate = now,
        initialPaymentAmount = None,
        scheduledPaymentEndDate = currentDate.plusMonths(6),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate = None,
        suspensionEndDate = None,
        balancingPaymentAmount = Some(60.00),
        balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
        totalLiability = Some(780.00),
        paymentPlanEditable = true
      )
    )

  val mockVariablePaymentPlanDetailResponse: PaymentPlanResponse =
    PaymentPlanResponse(
      directDebitDetails = DirectDebitDetails(
        bankSortCode = "123456",
        bankAccountNumber = "12345678",
        bankAccountName = "John Doe",
        auddisFlag = true
      ),
      paymentPlanDetails = PaymentPlanDetails(
        hodService = "NDD",
        planType = PaymentPlanType.VariablePaymentPlan.toString,
        paymentReference = "paymentReference",
        submissionDateTime = now.minusDays(5),
        scheduledPaymentAmount = 120.00,
        scheduledPaymentStartDate = currentDate.plusDays(5),
        initialPaymentStartDate = now,
        initialPaymentAmount = None,
        scheduledPaymentEndDate = currentDate.plusMonths(6),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate = None,
        suspensionEndDate = None,
        balancingPaymentAmount = Some(60.00),
        balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
        totalLiability = Some(780.00),
        paymentPlanEditable = true
      )
    )

  val mockTaxCreditRepaymentPlanDetailResponse: PaymentPlanResponse =
    PaymentPlanResponse(
      directDebitDetails = DirectDebitDetails(
        bankSortCode = "123456",
        bankAccountNumber = "12345678",
        bankAccountName = "John Doe",
        auddisFlag = true
      ),
      paymentPlanDetails = PaymentPlanDetails(
        hodService = "NDD",
        planType = PaymentPlanType.TaxCreditRepaymentPlan.toString,
        paymentReference = "paymentReference",
        submissionDateTime = now.minusDays(5),
        scheduledPaymentAmount = 120.00,
        scheduledPaymentStartDate = currentDate.plusDays(5),
        initialPaymentStartDate = now,
        initialPaymentAmount = None,
        scheduledPaymentEndDate = currentDate.plusMonths(6),
        scheduledPaymentFrequency = Some("Monthly"),
        suspensionStartDate = None,
        suspensionEndDate = None,
        balancingPaymentAmount = Some(60.00),
        balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
        totalLiability = Some(780.00),
        paymentPlanEditable = true
      )
    )
}


