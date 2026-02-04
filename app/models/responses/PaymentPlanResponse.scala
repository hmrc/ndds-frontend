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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import utils.Utils.{hodToDebitSourceMapping, numericToPaymentFrequencyMapping, numericToPlanTypeMapping}

import java.time.{LocalDate, LocalDateTime}

case class DirectDebitDetails(bankSortCode: Option[String],
                              bankAccountNumber: Option[String],
                              bankAccountName: Option[String],
                              auDdisFlag: Boolean,
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
  scheduledPaymentAmount: Option[BigDecimal],
  scheduledPaymentStartDate: Option[LocalDate],
  initialPaymentStartDate: Option[LocalDate],
  initialPaymentAmount: Option[BigDecimal],
  scheduledPaymentEndDate: Option[LocalDate],
  scheduledPaymentFrequency: Option[String],
  suspensionStartDate: Option[LocalDate],
  suspensionEndDate: Option[LocalDate],
  balancingPaymentAmount: Option[BigDecimal],
  balancingPaymentDate: Option[LocalDate],
  totalLiability: Option[BigDecimal],
  paymentPlanEditable: Boolean
)

object PaymentPlanDetails {

  implicit val reads: Reads[PaymentPlanDetails] = (
    (__ \ "hodService").read[String].map { code =>
      hodToDebitSourceMapping.getOrElse(code, code)
    } and
      (__ \ "planType").read[String].map { code =>
        numericToPlanTypeMapping.getOrElse(code, code)
      } and
      (__ \ "paymentReference").read[String] and
      (__ \ "submissionDateTime").read[LocalDateTime] and
      (__ \ "scheduledPaymentAmount").readNullable[BigDecimal] and
      (__ \ "scheduledPaymentStartDate").readNullable[LocalDate] and
      (__ \ "initialPaymentStartDate").readNullable[LocalDate] and
      (__ \ "initialPaymentAmount").readNullable[BigDecimal] and
      (__ \ "scheduledPaymentEndDate").readNullable[LocalDate] and
      (__ \ "scheduledPaymentFrequency").readNullable[JsValue].map {
        _.flatMap {
          case JsNumber(num)  => numericToPaymentFrequencyMapping.get(num.toInt)
          case JsString(code) => code.toIntOption.flatMap(numericToPaymentFrequencyMapping.get).orElse(Some(code))
          case _              => Some("unknownFrequency")
        }
      } and
      (__ \ "suspensionStartDate").readNullable[LocalDate] and
      (__ \ "suspensionEndDate").readNullable[LocalDate] and
      (__ \ "balancingPaymentAmount").readNullable[BigDecimal] and
      (__ \ "balancingPaymentDate").readNullable[LocalDate] and
      (__ \ "totalLiability").readNullable[BigDecimal] and
      (__ \ "paymentPlanEditable").read[Boolean]
  )(PaymentPlanDetails.apply _)

  implicit val writes: OWrites[PaymentPlanDetails] = Json.writes[PaymentPlanDetails]

  implicit val format: OFormat[PaymentPlanDetails] = OFormat(reads, writes)
}

case class PaymentPlanResponse(
  directDebitDetails: DirectDebitDetails,
  paymentPlanDetails: PaymentPlanDetails
)

object PaymentPlanResponse {
  implicit val format: OFormat[PaymentPlanResponse] = Json.format
}
