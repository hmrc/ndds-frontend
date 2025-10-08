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

package services

import com.google.inject.{Inject, Singleton}
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.SinglePaymentPlan
import models.audits.{AuditEvent, SubmitDirectDebitPaymentPlan}
import models.requests.DataRequest
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PaymentReferencePage}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  private val auditSource: String = "ndds-frontend"

  def sendEvent[A <: AuditEvent](auditEvent: A)(implicit hc: HeaderCarrier, writes: Writes[A], request: Request[?]): Unit = {
    val extendedDataEvent = ExtendedDataEvent(
      auditSource = auditSource,
      auditType   = auditEvent.auditType,
      detail      = Json.toJson(auditEvent),
      tags        = hc.toAuditTags(auditEvent.transactionName, request.uri)
    )

    auditConnector.sendExtendedEvent(extendedDataEvent)
  }

  def sendSubmitDirectDebitPaymentPlan(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Unit = {
    val paymentReference =
      request.userAnswers.get(PaymentReferencePage).getOrElse(throw new Exception("PaymentReferencePage details missing from user answers"))
    val paymentPlanSource =
      request.userAnswers.get(DirectDebitSourcePage).getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers"))

    val paymentPlanType = paymentPlanSource match {
      case MGD | TC | SA =>
        request.userAnswers.get(PaymentPlanTypePage).getOrElse(throw new Exception("PaymentPlanTypePage details missing from user answers"))
      case _ => SinglePaymentPlan
    }

    sendEvent(SubmitDirectDebitPaymentPlan(paymentReference, paymentPlanType))
  }
}
