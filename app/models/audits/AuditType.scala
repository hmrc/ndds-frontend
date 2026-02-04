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

package models.audits

import play.api.libs.json.*

sealed abstract class AuditType(val name: String)

case object NewDirectDebitAudit extends AuditType("NewDirectDebit")

case object AddPaymentPlanAudit extends AuditType("AddPaymentPlan")

case object AmendPaymentPlanAudit extends AuditType("AmendPaymentPlan")

case object SuspendPaymentPlanAudit extends AuditType("SuspendPaymentPlan")

case object AmendPaymentPlanSuspensionAudit extends AuditType("AmendPaymentPlanSuspension")

case object RemovePaymentPlanSuspensionAudit extends AuditType("RemovePaymentPlanSuspension")

case object CancelPaymentPlanAudit extends AuditType("CancelPaymentPlan")

object AuditType {

  private val all: Seq[AuditType] = Seq(
    NewDirectDebitAudit,
    AddPaymentPlanAudit,
    AmendPaymentPlanAudit,
    SuspendPaymentPlanAudit,
    AmendPaymentPlanSuspensionAudit,
    RemovePaymentPlanSuspensionAudit,
    CancelPaymentPlanAudit
  )

  private val lookup: Map[String, AuditType] = all.map(a => a.name -> a).toMap

  implicit val format: Format[AuditType] = Format(
    Reads {
      case JsString(value) =>
        lookup
          .get(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown AuditType: $value"))

      case _ => JsError("AuditType must be a string")
    },
    Writes(audit => JsString(audit.name))
  )
}
