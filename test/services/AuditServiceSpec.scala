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

import base.SpecBase
import models.PaymentPlanType.TaxCreditRepaymentPlan
import models.audits.SubmitDirectDebitPaymentPlan
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.{Headers, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.global

class AuditServiceSpec extends SpecBase {

  val mockConnector: AuditConnector = mock[AuditConnector]

  val testService = new AuditService(mockConnector)

  implicit val ec: ExecutionContext = global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Audit service" - {
    "sendEvent method" - {
      "must take an AuditEvent and send an the audit" in {
        val testUri = "testUri"
        implicit val request: Request[_] = FakeRequest("GET", testUri, Headers(), "")
        val testAuditEvent = SubmitDirectDebitPaymentPlan(paymentReference = "testReference", planType = TaxCreditRepaymentPlan)
        val extendedDataEvent = ExtendedDataEvent(
          auditSource = "ndds-frontend",
          auditType = testAuditEvent.auditType,
          detail = Json.toJson(testAuditEvent),
          tags = hc.toAuditTags(testAuditEvent.transactionName, testUri)
        )

        when(mockConnector.sendExtendedEvent(extendedDataEvent)).thenReturn(Future.successful(AuditResult.Success))

        testService.sendEvent(testAuditEvent) mustBe ((): Unit)
        verify(mockConnector).sendExtendedEvent(any())(any(), any())
      }
    }

    "sendSubmitDirectDebitPaymentPlan method" - {
      "must send an audit when needed data is gathered successfully and payment plan type is in the session" in {

      }
      "must send an audit when needed data is gathered successfully and payment plan type is NOT in the session" in {

      }
      "must fail when payment reference is missing from the session" in {

      }
      "must fail when plan source is missing from the session" in {

      }
      "must fail if payment plan type is missing, but is supposed to exist" in {

      }
    }
  }
}
