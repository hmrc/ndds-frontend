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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.http.Fault
import itutil.ApplicationWithWiremock
import models.requests.{ChrisSubmissionRequest, GenerateDdiRefRequest, PaymentPlanDuplicateCheckRequest, WorkingDaysOffsetRequest}
import models.responses.{BankAddress, Country, EarliestPaymentDate, GenerateDdiRefResponse}
import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, YourBankDetailsWithAuddisStatus}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

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
      val result = connector.getFutureWorkingDays(requestBody).futureValue

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
      val result = intercept[Exception](connector.getFutureWorkingDays(requestBody).futureValue)

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
      val result = intercept[Exception](connector.getFutureWorkingDays(requestBody).futureValue)

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
      val result = intercept[Exception](connector.getFutureWorkingDays(requestBody).futureValue)

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

  "submitChrisData" should {

    val planStartDateDetails = PlanStartDateDetails(
      enteredDate = LocalDate.of(2025, 9, 1),
      earliestPlanStartDate = "2025-09-01"
    )

    val paymentDateDetails = PaymentDateDetails(
      enteredDate = LocalDate.of(2025, 9, 15),
      earliestPaymentDate = "2025-09-01"
    )

    val submission = ChrisSubmissionRequest(
      serviceType = DirectDebitSource.TC,
      paymentPlanType = PaymentPlanType.TaxCreditRepaymentPlan,
      paymentFrequency = Some(PaymentsFrequency.Monthly),
      yourBankDetailsWithAuddisStatus = YourBankDetailsWithAuddisStatus(
        accountHolderName = "Test",
        sortCode = "123456",
        accountNumber = "12345678",
        auddisStatus = false,
        accountVerified = false
      ),
      planStartDate = Some(planStartDateDetails),
      planEndDate = None,
      paymentDate = Some(paymentDateDetails),
      yearEndAndMonth = None,
      bankDetailsAddress = BankAddress(
        lines = Seq("line 1"),
        town = "Town",
        country = Country("UK"),
        postCode = "NE5 2DH"
      ),
      ddiReferenceNo = "DDI123456789",
      paymentReference = "testReference",
      bankName = "Barclays",
      totalAmountDue = Some(BigDecimal(200)),
      paymentAmount = Some(BigDecimal(100.00)),
      regularPaymentAmount = Some(BigDecimal(90.00)),
      calculation = None
    )


    "successfully return true when CHRIS submission succeeds with 200 OK" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/chris"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("true")
          )
      )

      val result = connector.submitChrisData(submission).futureValue
      result shouldBe true
    }

    "must fail when CHRIS submission returns a non-200 status" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/chris"))
          .willReturn(
            aResponse().withStatus(BAD_REQUEST)
          )
      )

      val ex = intercept[Exception](connector.submitChrisData(submission).futureValue)
      ex.getMessage should include("CHRIS submission failed")
    }

    "must fail when CHRIS submission returns an UpstreamErrorResponse" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/chris"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("test error")
          )
      )

      val ex = intercept[Exception](connector.submitChrisData(submission).futureValue)
      ex.getMessage should include("status: 500")
    }

    "must fail when the result is a failed future" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/chris"))
          .willReturn(
            aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER) // Simulate connection drop
          )
      )

      val ex = intercept[Exception](connector.submitChrisData(submission).futureValue)
      ex.getMessage should include("The future returned an exception")
    }

  }

  "isDuplicatePaymentPlan" should {

    val duplicateCheckRequest: PaymentPlanDuplicateCheckRequest = PaymentPlanDuplicateCheckRequest(
      directDebitReference = "testRef",
      paymentPlanReference = "payment ref 123",
      planType = "type 1",
      paymentService = "CESA",
      paymentReference = "payment ref",
      paymentAmount = 120.00,
      totalLiability = 780.00,
      paymentFrequency = "WEEKLY"
    )

    "successfully return true when there is a duplicate Plan with 200 OK" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody("true") // JSON boolean
          )
      )

      val result = connector.isDuplicatePaymentPlan("testRef", duplicateCheckRequest).futureValue
      result shouldBe true
    }

    "must fail when request returns a non-200 status" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse().withStatus(BAD_REQUEST)
          )
      )

      val ex = intercept[Exception](connector.isDuplicatePaymentPlan("testRef",duplicateCheckRequest).futureValue)
      ex.getMessage should include("Failed")
    }

    "must fail when request returns an UpstreamErrorResponse" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("test error")
          )
      )

      val ex = intercept[Exception](connector.isDuplicatePaymentPlan("testRef",duplicateCheckRequest).futureValue)
      ex.getMessage should include("status: 500")
    }

    "must fail when the result is a failed future" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER) // Simulate connection drop
          )
      )

      val ex = intercept[Exception](connector.isDuplicatePaymentPlan("testRef",duplicateCheckRequest).futureValue)
      ex.getMessage should include("The future returned an exception")
    }
    
  }

}
