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

package models.audits

import models.PaymentPlanType
import models.PaymentPlanType.{BudgetPaymentPlan, SinglePayment, TaxCreditRepaymentPlan, VariablePaymentPlan}
import play.api.libs.json.*

sealed trait AuditEvent {
  val auditType: String
  val transactionName: String
}

case class GetDDIs() extends AuditEvent {
  override val auditType: String = "getDirectDebitInstructions"
  val transactionName: String = "get-direct-debit-instructions"
}

object GetDDIs {
  implicit val format: OFormat[GetDDIs] = Json.format
}

case class ConfirmBankDetails() extends AuditEvent {
  override val auditType: String = "confirmBankDetails"
  val transactionName: String = "confirm-bank-details"
}

object ConfirmBankDetails {
  implicit val format: OFormat[ConfirmBankDetails] = Json.format
}

case class SubmitDirectDebitPaymentPlan(paymentReference: String, planType: PaymentPlanType) extends AuditEvent {
  val auditType: String = "submitDirectDebitPaymentPlan"
  val transactionName: String = "submit-direct-debit-payment-plan"
}

object SubmitDirectDebitPaymentPlan {
  private implicit val paymentPlanTypeWrites: Writes[PaymentPlanType] = Writes {
    case SinglePayment => JsString("SINGLE")
    case VariablePaymentPlan => JsString("VPP")
    case BudgetPaymentPlan => JsString("BUDGET")
    case TaxCreditRepaymentPlan => JsString("TIME_TO_PAY")
  }

  implicit val format: Writes[SubmitDirectDebitPaymentPlan] = Json.writes[SubmitDirectDebitPaymentPlan]
}
