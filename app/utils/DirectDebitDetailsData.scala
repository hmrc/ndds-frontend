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

import models.responses.{NddDDPaymentPlansResponse, NddPaymentPlan, PaymentPlanDetailsResponse, PaymentPlanResponse}
import models.{DirectDebitDetails, NddDetails, NddResponse, PaymentPlanType, UKBankAddress}

import java.time.{LocalDate, LocalDateTime}

trait DirectDebitDetailsData {

  val directDebitDetailsData: List[DirectDebitDetails] = List(DirectDebitDetails(
    directDebitReference ="122222",
    setupDate="1 February 2024",
    sortCode = "666666",
    accountNumber = "00000000",
    paymentPlans = "0"
  ), DirectDebitDetails(
    directDebitReference ="133333",
    setupDate="2 March 2024",
    sortCode = "555555",
    accountNumber = "11111111",
    paymentPlans = "0"
  ),
    DirectDebitDetails(
      directDebitReference ="144444",
      setupDate="3 March 2024",
      sortCode = "333333",
      accountNumber = "22222222",
      paymentPlans = "0"
      
    ))

  val nddResponse: NddResponse = NddResponse(
    directDebitCount = 3,
    directDebitList = Seq(
      NddDetails(
        ddiRefNumber = "122222",
        submissionDateTime = LocalDateTime.parse("2024-02-01T00:00:00"),
        bankSortCode = "666666",
        bankAccountNumber = "00000000",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      ),
      NddDetails(
        ddiRefNumber = "133333",
        submissionDateTime = LocalDateTime.parse("2024-03-02T00:00:00"),
        bankSortCode = "555555",
        bankAccountNumber = "11111111",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      ),
      NddDetails(
        ddiRefNumber = "144444",
        submissionDateTime = LocalDateTime.parse("2024-03-03T00:00:00"),
        bankSortCode = "333333",
        bankAccountNumber = "22222222",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      )
    )
  )

  val ukBankAddress:UKBankAddress =UKBankAddress(
    addressLine1 =  "Address line 1",
    addressLine2 = Some( "Address line 2"),
    addressLine3 = Some( "Address line 3"),
    addressLine4 = Some( "Address line 4"),
    addressLine5 = Some( "Address line 5"),
    postCode = "TE1 2XR")

  val bankName="Test Bank Name"

  val mockDDPaymentPlansResponse: NddDDPaymentPlansResponse = NddDDPaymentPlansResponse(
    bankSortCode = "sort code",
    bankAccountNumber = "account number",
    bankAccountName = "account name",
    auDdisFlag = "dd",
    paymentPlanCount = 2,
    paymentPlanList = Seq(
      NddPaymentPlan(
        scheduledPaymentAmount = 100,
        planRefNumber = "ref number 1",
        planType = "type 1",
        paymentReference = "payment ref 1",
        hodService = "service 1",
        submissionDateTime = LocalDateTime.of(2025, 11, 12, 12, 12)),
      NddPaymentPlan(
        scheduledPaymentAmount = 100,
        planRefNumber = "ref number 1",
        planType = "type 1",
        paymentReference = "payment ref 1",
        hodService = "service 1",
        submissionDateTime = LocalDateTime.of(2025, 12, 12, 12, 12))
    )
  )

  private val now = LocalDateTime.now()

  private val currentDate = LocalDate.now()

  val mockSinglePaymentPlanDetailResponse: PaymentPlanDetailsResponse =
    PaymentPlanDetailsResponse(
      hodService = "NDD",
      planType = PaymentPlanType.SinglePaymentPlan.toString,
      paymentReference = "paymentReference",
      submissionDateTime = now.minusDays(5),
      scheduledPaymentAmount = 120.00,
      scheduledPaymentStartDate = currentDate.plusDays(5),
      initialPaymentStartDate = None,
      initialPaymentAmount = None,
      scheduledPaymentEndDate = currentDate.plusMonths(6),
      scheduledPaymentFrequency = Some("Monthly"),
      suspensionStartDate = None,
      suspensionEndDate = None,
      balancingPaymentAmount = Some("£60.00"),
      balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
      totalLiability = Some("£780.00"),
      paymentPlanEditable = true
    )

  val mockBudgetPaymentPlanDetailResponse: PaymentPlanDetailsResponse =
    PaymentPlanDetailsResponse(
      hodService = "NDD",
      planType = PaymentPlanType.BudgetPaymentPlan.toString,
      paymentReference = "paymentReference",
      submissionDateTime = now.minusDays(5),
      scheduledPaymentAmount = 120.00,
      scheduledPaymentStartDate = currentDate.plusDays(5),
      initialPaymentStartDate = None,
      initialPaymentAmount = None,
      scheduledPaymentEndDate = currentDate.plusMonths(6),
      scheduledPaymentFrequency = Some("Monthly"),
      suspensionStartDate = None,
      suspensionEndDate = None,
      balancingPaymentAmount = Some("£60.00"),
      balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
      totalLiability = Some("£780.00"),
      paymentPlanEditable = true
    )

  val mockVariablePaymentPlanDetailResponse: PaymentPlanDetailsResponse =
    PaymentPlanDetailsResponse(
      hodService = "NDD",
      planType = PaymentPlanType.VariablePaymentPlan.toString,
      paymentReference = "paymentReference",
      submissionDateTime = now.minusDays(5),
      scheduledPaymentAmount = 120.00,
      scheduledPaymentStartDate = currentDate.plusDays(5),
      initialPaymentStartDate = None,
      initialPaymentAmount = None,
      scheduledPaymentEndDate = currentDate.plusMonths(6),
      scheduledPaymentFrequency = Some("Monthly"),
      suspensionStartDate = None,
      suspensionEndDate = None,
      balancingPaymentAmount = Some("£60.00"),
      balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
      totalLiability = Some("£780.00"),
      paymentPlanEditable = true
    )

  val mockTaxCreditRepaymentPlanDetailResponse: PaymentPlanDetailsResponse =
    PaymentPlanDetailsResponse(
      hodService = "NDD",
      planType = PaymentPlanType.TaxCreditRepaymentPlan.toString,
      paymentReference = "paymentReference",
      submissionDateTime = now.minusDays(5),
      scheduledPaymentAmount = 120.00,
      scheduledPaymentStartDate = currentDate.plusDays(5),
      initialPaymentStartDate = None,
      initialPaymentAmount = None,
      scheduledPaymentEndDate = currentDate.plusMonths(6),
      scheduledPaymentFrequency = Some("Monthly"),
      suspensionStartDate = None,
      suspensionEndDate = None,
      balancingPaymentAmount = Some("£60.00"),
      balancingPaymentDate = Some(now.plusMonths(6).plusDays(10)),
      totalLiability = Some("£780.00"),
      paymentPlanEditable = true
    )
}


