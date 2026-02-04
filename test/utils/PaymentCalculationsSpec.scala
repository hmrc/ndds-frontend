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

package utils

import org.scalatestplus.play.PlaySpec

import java.time.LocalDate

class PaymentCalculationsSpec extends PlaySpec {

  private val TotalAmountEven = BigDecimal(1200)
  private val PaymentsEven = 12
  private val ExpectedEvenPayment = 100.00

  private val TotalAmountUneven = BigDecimal(1000)
  private val PaymentsUneven = 3
  private val ExpectedUnevenPayment = 333.33

  private val FinalPaymentBaseAmount = BigDecimal(1000)
  private val FinalRegularPayment = BigDecimal(333.33)
  private val FinalRegularPaymentCount = 2
  private val ExpectedFinalPaymentAmount = BigDecimal(333.34)

  private val StartDate = LocalDate.of(2025, 1, 15)
  private val OffsetOne = 1
  private val OffsetTen = 10
  private val OffsetEleven = 11

  private val ExpectedSecondPaymentDate = LocalDate.of(2025, 2, 15)
  private val ExpectedPenultimatePaymentDate = LocalDate.of(2025, 11, 15)
  private val ExpectedFinalPaymentDate = LocalDate.of(2025, 12, 15)

  "PaymentCalculations" should {

    "correctly calculate the regular payment amount for evenly divisible amount" in {
      val result = PaymentCalculations.calculateRegularPaymentAmount(TotalAmountEven, PaymentsEven)
      result mustBe ExpectedEvenPayment
    }

    "correctly calculate the regular payment amount for uneven division" in {
      val result = PaymentCalculations.calculateRegularPaymentAmount(TotalAmountUneven, PaymentsUneven)
      result mustBe ExpectedUnevenPayment
    }

    "calculate the final payment correctly when regular payments leave a remainder" in {
      val result = PaymentCalculations.calculateFinalPayment(FinalPaymentBaseAmount, FinalRegularPayment, FinalRegularPaymentCount)
      result mustBe ExpectedFinalPaymentAmount
    }

    "return the correct second payment date" in {
      val result = PaymentCalculations.calculateSecondPaymentDate(StartDate, OffsetOne)
      result mustBe ExpectedSecondPaymentDate
    }

    "return the correct penultimate payment date" in {
      val result = PaymentCalculations.calculatePenultimatePaymentDate(StartDate, OffsetTen)
      result mustBe ExpectedPenultimatePaymentDate
    }

    "return the correct final payment date" in {
      val result = PaymentCalculations.calculateFinalPaymentDate(StartDate, OffsetEleven)
      result mustBe ExpectedFinalPaymentDate
    }
  }
}
