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

package utils

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

object PaymentCalculations {
  private val ScaleTwo = 2
  private val ScaleFour = 4
  def calculateRegularPaymentAmount(
    totalAmountDueInput: BigDecimal,
    totalNumberOfPayments: Int
  ): Double = {
    val totalAmountDueRounded = totalAmountDueInput.setScale(ScaleTwo, RoundingMode.HALF_DOWN)

    val regularPaymentFullPrecision =
      (totalAmountDueRounded / BigDecimal(totalNumberOfPayments))
        .setScale(ScaleFour, RoundingMode.FLOOR)

    val regularPaymentFinal =
      regularPaymentFullPrecision.setScale(ScaleTwo, RoundingMode.DOWN)

    regularPaymentFinal.toDouble
  }

  def calculateFinalPayment(
    totalAmountDue: BigDecimal,
    regularPaymentAmount: BigDecimal,
    numberOfEqualPayments: Int
  ): BigDecimal = {
    val totalRegularPayments = regularPaymentAmount * BigDecimal(numberOfEqualPayments)
    totalAmountDue - totalRegularPayments
  }

  def calculateSecondPaymentDate(
    planStartDate: LocalDate,
    monthsOffset: Int
  ): LocalDate = {
    planStartDate.plusMonths(monthsOffset.toLong)
  }

  def calculatePenultimatePaymentDate(
    planStartDate: LocalDate,
    penultimateInstallmentOffset: Int
  ): LocalDate = {
    planStartDate.plusMonths(penultimateInstallmentOffset.toLong)
  }
  def calculateFinalPaymentDate(
    planStartDate: LocalDate,
    monthsOffset: Int
  ): LocalDate = {
    planStartDate.plusMonths(monthsOffset.toLong)
  }
}
