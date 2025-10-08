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
import models.DirectDebitSource.{MGD, OL}
import models.PaymentPlanType.{TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.UserAnswers
import models.audits.SubmitDirectDebitPaymentPlan
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PaymentReferencePage}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Headers, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends SpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  val mockConnector: AuditConnector = mock[AuditConnector]

  val testService = new AuditService(mockConnector)

  implicit val ec: ExecutionContext = global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testUri = "testUri"

  "Audit service" - {
    "sendEvent method" - {
      "must take an AuditEvent and send an the audit" in {
        implicit val request: Request[?] = FakeRequest("GET", testUri, Headers(), "")
        val testAuditEvent = SubmitDirectDebitPaymentPlan(paymentReference = "testReference", planType = TaxCreditRepaymentPlan)
        val extendedDataEvent = ExtendedDataEvent(
          auditSource = "ndds-frontend",
          auditType   = testAuditEvent.auditType,
          detail      = Json.toJson(testAuditEvent),
          tags        = hc.toAuditTags(testAuditEvent.transactionName, testUri)
        )

        when(mockConnector.sendExtendedEvent(extendedDataEvent)).thenReturn(Future.successful(AuditResult.Success))

        testService.sendEvent(testAuditEvent) mustBe ((): Unit)
        verify(mockConnector).sendExtendedEvent(any())(any(), any())
      }
    }

    "sendSubmitDirectDebitPaymentPlan method" - {
      "must send an audit when needed data is gathered successfully and payment plan type is in the session" in {
        val request: Request[AnyContent] = FakeRequest("GET", testUri, Headers(), AnyContent("test"))
        val expectedUserAnswers: UserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, MGD)
          .success
          .value
          .set(PaymentPlanTypePage, VariablePaymentPlan)
          .success
          .value
          .set(PaymentReferencePage, "testPaymentReference")
          .success
          .value

        implicit val dataRequest: DataRequest[AnyContent] = DataRequest(request, "testUserId", expectedUserAnswers)

        when(mockConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        testService.sendSubmitDirectDebitPaymentPlan(hc, dataRequest) mustBe ((): Unit)
        verify(mockConnector).sendExtendedEvent(any())(any(), any())
      }
      "must send an audit when needed data is gathered successfully and payment plan type is NOT in the session" in {
        val request: Request[AnyContent] = FakeRequest("GET", testUri, Headers(), AnyContent("test"))
        val expectedUserAnswers: UserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, OL)
          .success
          .value
          .set(PaymentReferencePage, "testPaymentReference")
          .success
          .value

        implicit val dataRequest: DataRequest[AnyContent] = DataRequest(request, "testUserId", expectedUserAnswers)

        when(mockConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        testService.sendSubmitDirectDebitPaymentPlan(hc, dataRequest) mustBe ((): Unit)
        verify(mockConnector).sendExtendedEvent(any())(any(), any())
      }
      "must fail when payment reference is missing from the session" in {
        val request: Request[AnyContent] = FakeRequest("GET", testUri, Headers(), AnyContent("test"))
        val expectedUserAnswers: UserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, OL)
          .success
          .value

        implicit val dataRequest: DataRequest[AnyContent] = DataRequest(request, "testUserId", expectedUserAnswers)

        val result = intercept[Exception](testService.sendSubmitDirectDebitPaymentPlan(hc, dataRequest))

        result.getMessage must include("PaymentReferencePage details missing from user answers")
      }
      "must fail when plan source is missing from the session" in {
        val request: Request[AnyContent] = FakeRequest("GET", testUri, Headers(), AnyContent("test"))
        val expectedUserAnswers: UserAnswers = emptyUserAnswers
          .set(PaymentReferencePage, "testPaymentReference")
          .success
          .value

        implicit val dataRequest: DataRequest[AnyContent] = DataRequest(request, "testUserId", expectedUserAnswers)

        val result = intercept[Exception](testService.sendSubmitDirectDebitPaymentPlan(hc, dataRequest))

        result.getMessage must include("DirectDebitSourcePage details missing from user answers")
      }
      "must fail if payment plan type is missing, but is supposed to exist" in {
        val request: Request[AnyContent] = FakeRequest("GET", testUri, Headers(), AnyContent("test"))
        val expectedUserAnswers: UserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, MGD)
          .success
          .value
          .set(PaymentReferencePage, "testPaymentReference")
          .success
          .value

        implicit val dataRequest: DataRequest[AnyContent] = DataRequest(request, "testUserId", expectedUserAnswers)

        val result = intercept[Exception](testService.sendSubmitDirectDebitPaymentPlan(hc, dataRequest))

        result.getMessage must include("PaymentPlanTypePage details missing from user answers")
      }
    }
  }
}
