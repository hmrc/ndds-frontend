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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor, urlEqualTo, urlPathMatching}
import itutil.ApplicationWithWiremock
import models.requests.{GenerateDdiRefRequest, WorkingDaysOffsetRequest}
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier

class NationalDirectDebitConnectorSpec extends ApplicationWithWiremock
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: NationalDirectDebitConnector = app.injector.instanceOf[NationalDirectDebitConnector]

  "getEarliestPaymentDate" should {
    "successfully retrieve a date" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/future-working-days"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{"date":"2024-12-28"}""")
          )
      )

      val requestBody = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = 3)
      val result = connector.getEarliestPaymentDate(requestBody).futureValue

      result shouldBe EarliestPaymentDate("2024-12-28")
    }

    "must fail when the result is parsed as a HttpResponse but is not a 200 (OK) response" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/future-working-days"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
          )
      )

      val requestBody = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = 3)
      val result = intercept[Exception](connector.getEarliestPaymentDate(requestBody).futureValue)

      result.getMessage should include("Unexpected status code: 201")
    }

    "must fail when the result is parsed as an UpstreamErrorResponse" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/future-working-days"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("test error")
          )
      )

      val requestBody = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = 3)
      val result = intercept[Exception](connector.getEarliestPaymentDate(requestBody).futureValue)

      result.getMessage should include("Response body: 'test error', status code: 500")
    }

    "must fail when the result is a failed future" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/future-working-days"))
          .willReturn(
            aResponse()
              .withStatus(0)
          )
      )

      val requestBody = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = 3)
      val result = intercept[Exception](connector.getEarliestPaymentDate(requestBody).futureValue)

      result.getMessage should include("The future returned an exception")
    }
  }

  "generateNewDdiReference" should {
    "successfully retrieve ddi reference number" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debit-reference"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{"ddiRefNumber":"testRef"}""")
          )
      )

      val requestBody = GenerateDdiRefRequest("testRef")
      val result = connector.generateNewDdiReference(requestBody).futureValue

      result shouldBe GenerateDdiRefResponse("testRef")
    }

    "must fail when the result is parsed as a HttpResponse but is not a 200 (OK) response" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debit-reference"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
          )
      )

      val requestBody = GenerateDdiRefRequest("testRef")
      val result = intercept[Exception](connector.generateNewDdiReference(requestBody).futureValue)

      result.getMessage should include("Unexpected status code: 201")
    }

    "must fail when the result is parsed as an UpstreamErrorResponse" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debit-reference"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("test error")
          )
      )

      val requestBody = GenerateDdiRefRequest("testRef")
      val result = intercept[Exception](connector.generateNewDdiReference(requestBody).futureValue)

      result.getMessage should include("Response body: 'test error', status code: 500")
    }

    "must fail when the result is a failed future" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debit-reference"))
          .willReturn(
            aResponse()
              .withStatus(0)
          )
      )

      val requestBody = GenerateDdiRefRequest("testRef")
      val result = intercept[Exception](connector.generateNewDdiReference(requestBody).futureValue)

      result.getMessage should include("The future returned an exception")
    }
  }

  "retrieveDirectDebitPaymentPlans" should {
    "successfully retrieve direct debit payment plans" in {
      val responseJson =
        """
          |{
          |  "bankSortCode": "123456",
          |  "bankAccountNumber": "12345678",
          |  "bankAccountName": "Test Name",
          |  "auDdisFlag": "Y",
          |  "paymentPlanCount": 1,
          |  "paymentPlanList": [
          |    {
          |      "scheduledPaymentAmount": 100.50,
          |      "planRefNumber": "plan-123",
          |      "planType": "STANDARD",
          |      "paymentReference": "pay-123",
          |      "hodService": "HMRC",
          |      "submissionDateTime": "2024-09-15T10:15:30"
          |    }
          |  ]
          |}
          |""".stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/testRef/payment-plans"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.retrieveDirectDebitPaymentPlans("testRef").futureValue

      result.bankSortCode shouldBe "123456"
      result.paymentPlanCount shouldBe 1
      result.paymentPlanList.head.planRefNumber shouldBe "plan-123"
    }
  }

  "retrieveDirectDebits" should {
    "successfully retrieve direct debits" in {
      val responseJson =
        """
          |{
          |  "directDebitCount": 2,
          |  "directDebitList": [
          |    {
          |      "ddiRefNumber": "DDI123456",
          |      "submissionDateTime": "2025-09-16T10:15:30",
          |      "bankSortCode": "123456",
          |      "bankAccountNumber": "12345678",
          |      "bankAccountName": "John Doe",
          |      "auDdisFlag": true,
          |      "numberOfPayPlans": 2
          |    },
          |    {
          |      "ddiRefNumber": "DDI654321",
          |      "submissionDateTime": "2025-09-16T11:45:00",
          |      "bankSortCode": "654321",
          |      "bankAccountNumber": "87654321",
          |      "bankAccountName": "Jane Smith",
          |      "auDdisFlag": false,
          |      "numberOfPayPlans": 1
          |    }
          |  ]
          |}
          |""".stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.retrieveDirectDebits().futureValue

      result.directDebitCount shouldBe 2
    }
  }
}
