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

package models.requests

import models.{DirectDebitSource, PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.*
import play.api.libs.json.{Json, OFormat}
import queries.*
import utils.Utils.{debitSourceToHodMapping, paymentFrequencyToNumericMapping, planTypeToNumericMapping}

import java.time.LocalDate

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

  def build(userAnswers: UserAnswers, paymentAmount: Option[BigDecimal], paymentStartDate: Option[LocalDate]): PaymentPlanDuplicateCheckRequest = {
    val existingDd = userAnswers
      .get(ExistingDirectDebitIdentifierQuery)
      .getOrElse(throw new RuntimeException("Missing ExistingDirectDebitIdentifierQuery"))

    val planType = userAnswers
      .get(PaymentPlanTypePage)
      .map(_.toString)
      .getOrElse(PaymentPlanType.SinglePaymentPlan.toString)

    val hodService = userAnswers
      .get(DirectDebitSourcePage)
      .flatMap(value => debitSourceToHodMapping.get(value.toString))
      .getOrElse(throw new RuntimeException("Missing DirectDebitSourcePage"))

    val paymentReference = userAnswers
      .get(PaymentReferencePage)
      .getOrElse(throw new RuntimeException("Missing PaymentReferencePage"))

    val frequency = userAnswers.get(PaymentsFrequencyPage).flatMap(value => paymentFrequencyToNumericMapping.get(value.toString))

    val amount = planType match {
      case PaymentPlanType.BudgetPaymentPlan.toString => userAnswers.get(RegularPaymentAmountPage)
      case _                                          => userAnswers.get(PaymentAmountPage)
    }

    val startDate = planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        userAnswers
          .get(PaymentDatePage)
          .map(_.enteredDate)
          .getOrElse(throw new RuntimeException("Missing PaymentDatePage"))
      case _ =>
        userAnswers
          .get(PlanStartDatePage)
          .map(_.enteredDate)
          .getOrElse(throw new RuntimeException("Missing PlanStartDatePage"))
    }

    PaymentPlanDuplicateCheckRequest(
      directDebitReference = existingDd.ddiRefNumber,
      paymentPlanReference = "",
      planType = userAnswers
        .get(PaymentPlanTypePage)
        .map(_.toString)
        .flatMap(planTypeToNumericMapping.get)
        .getOrElse(PaymentPlanType.SinglePaymentPlan.toString),
      paymentService   = hodService,
      paymentReference = paymentReference,
      paymentAmount    = amount,
      totalLiability   = userAnswers.get(TotalAmountDuePage),
      paymentFrequency = frequency,
      paymentStartDate = startDate
    )
  }
}
