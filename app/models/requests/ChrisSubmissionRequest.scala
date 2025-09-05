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

import models.responses.BankAddress
import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanCalculation, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, YearEndAndMonth, YourBankDetails, YourBankDetailsWithAuddisStatus}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ChrisSubmissionRequest(
                                   serviceType: DirectDebitSource,
                                   paymentPlanType: PaymentPlanType,
                                   paymentFrequency: Option[PaymentsFrequency],            // optional
                                   yourBankDetailsWithAuddisStatus: YourBankDetailsWithAuddisStatus,
                                   auddisStatus: Option[Boolean],
                                   planStartDate: Option[PlanStartDateDetails],                       // optional
                                   planEndDate: Option[LocalDate],                       // optional
                                   paymentDate: Option[PaymentDateDetails],                         // optional
                                   yearEndAndMonth: Option[YearEndAndMonth],              // optional
                                   bankDetails: YourBankDetails,
                                   bankDetailsAddress: BankAddress,
                                   ddiReferenceNo: String,
                                   paymentReference: Option[String],
                                   bankName: String,
                                   totalAmountDue:Option[BigDecimal],
                                   paymentAmount:Option[BigDecimal],
                                   regularPaymentAmount:Option[BigDecimal],
                                   calculation: Option[PaymentPlanCalculation]            // optional
                                 )

object ChrisSubmissionRequest {
  implicit val format: OFormat[ChrisSubmissionRequest] = Json.format[ChrisSubmissionRequest]
}
