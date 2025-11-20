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

import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanCalculation, PaymentPlanType, PersonalOrBusinessAccount, PlanStartDateDetails, SuspensionPeriodRange, YearEndAndMonth, YourBankDetailsWithAuddisStatus}
import models.audits.AuditType
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ChrisSubmissionRequest(
  serviceType: DirectDebitSource,
  paymentPlanType: PaymentPlanType,
  paymentPlanReferenceNumber: Option[String],
  paymentFrequency: Option[String],
  yourBankDetailsWithAuddisStatus: YourBankDetailsWithAuddisStatus,
  planStartDate: Option[PlanStartDateDetails],
  planEndDate: Option[LocalDate],
  paymentDate: Option[PaymentDateDetails],
  yearEndAndMonth: Option[YearEndAndMonth],
  ddiReferenceNo: String,
  paymentReference: String,
  totalAmountDue: Option[BigDecimal],
  paymentAmount: Option[BigDecimal],
  regularPaymentAmount: Option[BigDecimal],
  amendPaymentAmount: Option[BigDecimal],
  calculation: Option[PaymentPlanCalculation],
  suspensionPeriodRangeDate: Option[SuspensionPeriodRange],
  amendPlan: Boolean = false,
  cancelPlan: Boolean = false,
  suspendPlan: Boolean = false,
  removeSuspensionPlan: Boolean = false,
  auditType: Option[AuditType] = None,
  bankAccountType: Option[PersonalOrBusinessAccount] = None
)

object ChrisSubmissionRequest {
  implicit val format: OFormat[ChrisSubmissionRequest] = Json.format[ChrisSubmissionRequest]
}
