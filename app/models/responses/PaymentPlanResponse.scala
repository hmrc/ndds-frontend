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

package models.responses

import java.time.{LocalDate, LocalDateTime}
import play.api.libs.json.{Json, OFormat}

case class DirectDebitDetails(
                               bankSortCode: String,
                               bankAccountNumber: String,
                               bankAccountName: String,
                               auddisFlag: Boolean,
                               submissionDateTime: LocalDateTime
                             )

object DirectDebitDetails {
  implicit val format: OFormat[DirectDebitDetails] = Json.format
}

case class PaymentPlanDetails(
                               hodService: String,
                               planType: String,
                               paymentReference: String,
                               submissionDateTime: LocalDateTime,
                               scheduledPaymentAmount: Double,
                               scheduledPaymentStartDate: LocalDate,
                               initialPaymentStartDate: LocalDate,
                               initialPaymentAmount: BigDecimal,
                               scheduledPaymentEndDate: LocalDate,
                               scheduledPaymentFrequency: String,
                               suspensionStartDate: LocalDate,
                               suspensionEndDate: LocalDate,
                               balancingPaymentAmount: BigDecimal,
                               balancingPaymentDate: LocalDate,
                               totalLiability: BigDecimal,
                               paymentPlanEditable: Boolean
                             )

object PaymentPlanDetails {
  implicit val format: OFormat[PaymentPlanDetails] = Json.format
}

case class PaymentPlanResponse(
                                directDebitDetails: DirectDebitDetails,
                                paymentPlanDetails: PaymentPlanDetails
                              )

object PaymentPlanResponse {
  implicit val format: OFormat[PaymentPlanResponse] = Json.format
}