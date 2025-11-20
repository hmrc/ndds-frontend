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

package models.requests

import models.{PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.*
import play.api.libs.json.{Json, OFormat}
import queries.*

import java.time.LocalDate
import scala.concurrent.Future

case class PaymentPlanDuplicateCheckRequest(
  directDebitReference: String,
  paymentPlanReference: String,
  planType: String,
  paymentService: String,
  paymentReference: String,
  paymentAmount: Option[BigDecimal],
  totalLiability: Option[BigDecimal],
  paymentFrequency: Option[Int],
  paymentStartDate: LocalDate
)

object PaymentPlanDuplicateCheckRequest {
  implicit val format: OFormat[PaymentPlanDuplicateCheckRequest] = Json.format[PaymentPlanDuplicateCheckRequest]

  def build(userAnswers: UserAnswers,
            paymentAmount: Option[BigDecimal],
            paymentStartDate: Option[LocalDate],
            isAmendPlan: Boolean
           ): PaymentPlanDuplicateCheckRequest = {
    // TODO Need to set according to payment plan
    if (isAmendPlan) {
      PaymentPlanDuplicateCheckRequest("ddRef", "planRef", "hod", "planType", "paymentRef", Some(100), Some(200), Some(1), LocalDate.now())
    } else { // Adding new payment plan

      val directDebitReference = userAnswers.get(DirectDebitReferenceQuery).get // TODO changed to ExistingDirectDebitIdentifierQuery
      val paymentPlanReference = ""
      val planType = userAnswers.get(PaymentPlanTypePage).get.toString
      val hodService = userAnswers.get(DirectDebitSourcePage).get.toString
      val paymentReference = userAnswers.get(PaymentReferencePage).get
      val amount = userAnswers.get(PaymentAmountPage).get
      val liability = userAnswers.get(TotalAmountDuePage).get
      val frequency = userAnswers.get(PaymentsFrequencyPage).get.toString
      val startDate = userAnswers.get(PlanStartDatePage).get.enteredDate

      PaymentPlanDuplicateCheckRequest(
        directDebitReference = directDebitReference,
        paymentPlanReference = paymentPlanReference,
        planType             = planType,
        paymentService       = hodService,
        paymentReference     = paymentReference,
        paymentAmount        = Some(amount),
        totalLiability       = Some(liability),
        paymentFrequency     = PaymentsFrequency.paymentFrequencyMapping.get(frequency),
        paymentStartDate     = startDate
      )
    }
  }
}
