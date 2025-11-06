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

import models.{DirectDebitSource, PaymentPlanType}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDateTime}

case class NddPaymentPlan(scheduledPaymentAmount: BigDecimal,
                          planRefNumber: String,
                          planType: String,
                          paymentReference: String,
                          hodService: String,
                          submissionDateTime: LocalDateTime
                         )

object NddPaymentPlan {
  private val planTypeMapping: Map[String, String] = Map(
    "01" -> PaymentPlanType.SinglePaymentPlan.toString,
    "02" -> PaymentPlanType.BudgetPaymentPlan.toString,
    "03" -> PaymentPlanType.TaxCreditRepaymentPlan.toString,
    "04" -> PaymentPlanType.VariablePaymentPlan.toString
  )

  implicit val reads: Reads[NddPaymentPlan] = (
    (__ \ "scheduledPaymentAmount").read[BigDecimal] and
      (__ \ "planRefNumber").read[String] and
      (__ \ "planType").read[String].map(code => planTypeMapping.getOrElse(code, "unknownPlanType")) and
      (__ \ "paymentReference").read[String] and
      (__ \ "hodService").read[String].map { code =>
        DirectDebitSource.hodServiceMapping.getOrElse(code, code)
      } and
      (__ \ "submissionDateTime").read[LocalDateTime]
  )(NddPaymentPlan.apply _)

  implicit val writes: OWrites[NddPaymentPlan] = Json.writes[NddPaymentPlan]

  implicit val format: OFormat[NddPaymentPlan] = OFormat(reads, writes)
}

case class NddDDPaymentPlansResponse(bankSortCode: String,
                                     bankAccountNumber: String,
                                     bankAccountName: String,
                                     auDdisFlag: String,
                                     paymentPlanCount: Int,
                                     paymentPlanList: Seq[NddPaymentPlan]
                                    )

object NddDDPaymentPlansResponse {
  implicit val format: OFormat[NddDDPaymentPlansResponse] = Json.format[NddDDPaymentPlansResponse]
}

case class PaymentPlanDAO(id: String, lastUpdated: Instant, paymentPlans: Seq[NddPaymentPlan])

object PaymentPlanDAO {
  import NddPaymentPlan.format
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[PaymentPlanDAO] = Json.format[PaymentPlanDAO]
}
