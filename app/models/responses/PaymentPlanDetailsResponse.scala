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

import play.api.libs.json.*

import java.time.{LocalDateTime, LocalDate}
import java.time.format.DateTimeFormatter

implicit val localDateTimeFormat: Format[LocalDateTime] = {
  val fmt = DateTimeFormatter.ISO_DATE_TIME
  Format(Reads.localDateTimeReads(fmt), Writes.temporalWrites(fmt))
}

case class PaymentPlanDetailsResponse(
                               hodService: String,
                               planType: String,
                               paymentReference: String,
                               submissionDateTime: LocalDateTime,
                               scheduledPaymentAmount: BigDecimal,
                               scheduledPaymentStartDate: LocalDate,
                               initialPaymentStartDate: Option[LocalDateTime],
                               initialPaymentAmount: Option[BigDecimal],
                               scheduledPaymentEndDate: LocalDate,
                               scheduledPaymentFrequency: Option[String],
                               suspensionStartDate: Option[LocalDateTime],
                               suspensionEndDate: Option[LocalDateTime],
                               balancingPaymentAmount: Option[BigDecimal],
                               balancingPaymentDate: Option[LocalDateTime],
                               totalLiability: Option[BigDecimal],
                               paymentPlanEditable: Boolean
                             )

object PaymentPlanDetailsResponse {
  implicit val format: OFormat[PaymentPlanDetailsResponse] = Json.format[PaymentPlanDetailsResponse]
}
