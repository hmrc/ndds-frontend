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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import itutil.ApplicationWithWiremock
import models.requests.{ChrisSubmissionRequest, GenerateDdiRefRequest, PaymentPlanDuplicateCheckRequest, WorkingDaysOffsetRequest}
import models.responses.*
import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, YourBankDetailsWithAuddisStatus}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class NationalDirectDebitConnectorSpec extends ApplicationWithWiremock with Matchers with ScalaFutures with IntegrationPatience {

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
          |  "paymentPlanCount": 4,
          |  "paymentPlanList": [
          |    {
          |      "scheduledPaymentAmount": 100.50,
          |      "planRefNumber": "plan-123",
          |      "planType": "01",
          |      "paymentReference": "pay-123",
          |      "hodService": "HMRC",
          |      "submissionDateTime": "2024-09-15T10:15:30"
          |    },
          |    {
          |      "scheduledPaymentAmount": 100.50,
          |      "planRefNumber": "plan-123",
          |      "planType": "02",
          |      "paymentReference": "pay-123",
          |      "hodService": "HMRC",
          |      "submissionDateTime": "2024-09-15T10:15:30"
          |    },
          |    {
          |      "scheduledPaymentAmount": 100.50,
          |      "planRefNumber": "plan-123",
          |      "planType": "03",
          |      "paymentReference": "pay-123",
          |      "hodService": "HMRC",
          |      "submissionDateTime": "2024-09-15T10:15:30"
          |    },
          |    {
          |      "scheduledPaymentAmount": 100.50,
          |      "planRefNumber": "plan-123",
          |      "planType": "04",
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

      result.bankSortCode                  shouldBe "123456"
      result.paymentPlanCount              shouldBe 4
      result.paymentPlanList.head.planType shouldBe PaymentPlanType.SinglePaymentPlan.toString
      result.paymentPlanList(1).planType   shouldBe PaymentPlanType.BudgetPaymentPlan.toString
      result.paymentPlanList(2).planType   shouldBe PaymentPlanType.TaxCreditRepaymentPlan.toString
      result.paymentPlanList(3).planType   shouldBe PaymentPlanType.VariablePaymentPlan.toString
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
      enteredDate           = LocalDate.of(2025, 9, 1),
      earliestPlanStartDate = "2025-09-01"
    )

    val paymentDateDetails = PaymentDateDetails(
      enteredDate         = LocalDate.of(2025, 9, 15),
      earliestPaymentDate = "2025-09-01"
    )

    val submission = ChrisSubmissionRequest(
      serviceType = DirectDebitSource.TC,
      paymentPlanType = PaymentPlanType.TaxCreditRepaymentPlan,
      paymentPlanReferenceNumber = None,
      paymentFrequency = Some(PaymentsFrequency.Monthly.toString),
      yourBankDetailsWithAuddisStatus = YourBankDetailsWithAuddisStatus(
        accountHolderName = "Test",
        sortCode          = "123456",
        accountNumber     = "12345678",
        auddisStatus      = false,
        accountVerified   = false
      ),
      planStartDate   = Some(planStartDateDetails),
      planEndDate     = None,
      paymentDate     = Some(paymentDateDetails),
      yearEndAndMonth = None,
      ddiReferenceNo = "DDI123456789",
      paymentReference = "testReference",
      totalAmountDue = Some(BigDecimal(200)),
      paymentAmount = Some(BigDecimal(100.00)),
      regularPaymentAmount = Some(BigDecimal(90.00)),
      amendPaymentAmount = None,
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

  "getPaymentPlanDetails" should {
    "successfully retrieve payment plan details" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "CESA",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "1",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
        """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.directDebitDetails.bankSortCode    shouldBe Some("123456")
      result.directDebitDetails.bankAccountName shouldBe None
      result.directDebitDetails.auDdisFlag      shouldBe true

      result.paymentPlanDetails.hodService        shouldBe DirectDebitSource.SA.toString
      result.paymentPlanDetails.planType          shouldBe PaymentPlanType.SinglePaymentPlan.toString
      result.paymentPlanDetails.suspensionEndDate shouldBe None
      result.paymentPlanDetails.totalLiability    shouldBe None
    }

    "successfully retrieve payment plan details when scheduledPaymentFrequency is null" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "NIDN",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": null,
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
            """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService                shouldBe DirectDebitSource.NIC.toString
      result.paymentPlanDetails.scheduledPaymentFrequency shouldBe None
    }

    "successfully retrieve payment plan details when scheduledPaymentFrequency is weekly" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "unknownHodService",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService                shouldBe "unknownHodService"
      result.paymentPlanDetails.scheduledPaymentFrequency shouldBe Some("weekly")
    }

    "successfully retrieve payment plan details when hodService is COTA" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "COTA",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                    """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.CT.toString
    }

    "successfully retrieve payment plan details when hodService is NIDN" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "NIDN",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                        """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.NIC.toString
    }

    "successfully retrieve payment plan details when hodService is SAFE" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "SAFE",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                            """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.OL.toString
    }

    "successfully retrieve payment plan details when hodService is PAYE" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "PAYE",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.PAYE.toString
    }

    "successfully retrieve payment plan details when hodService is CESA" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "CESA",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                    """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.SA.toString
    }

    "successfully retrieve payment plan details when hodService is SDLT" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "SDLT",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                        """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.SDLT.toString
    }

    "successfully retrieve payment plan details when hodService is NTC" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "NTC",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                            """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.TC.toString
    }

    "successfully retrieve payment plan details when hodService is VAT" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "VAT",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                                """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.VAT.toString
    }

    "successfully retrieve payment plan details when hodService is MGD" in {

      val responseJson =
        """
          |{
          |  "directDebitDetails": {
          |    "bankSortCode": "123456",
          |    "bankAccountNumber": "12345678",
          |    "bankAccountName": null,
          |    "auDdisFlag": true,
          |    "submissionDateTime": "2025-09-30T11:00:00"
          |  },
          |  "paymentPlanDetails": {
          |    "hodService": "MGD",
          |    "planType": "01",
          |    "paymentReference": "test-pp-ref",
          |    "submissionDateTime": "2025-09-30T11:00:00",
          |    "scheduledPaymentAmount": 1000,
          |    "scheduledPaymentStartDate": "2025-10-01",
          |    "initialPaymentStartDate": "2025-10-01",
          |    "initialPaymentAmount": 150,
          |    "scheduledPaymentEndDate": "2025-10-01",
          |    "scheduledPaymentFrequency": "weekly",
          |    "suspensionStartDate": "2025-11-01",
          |    "suspensionEndDate": null,
          |    "balancingPaymentAmount": 600,
          |    "balancingPaymentDate": "2025-12-01",
          |    "totalLiability": null,
          |    "paymentPlanEditable": false
          |  }
          |}
                                                    """.stripMargin

      stubFor(
        get(urlEqualTo("/national-direct-debit/direct-debits/test-dd-ref/payment-plans/test-pp-ref"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(responseJson)
          )
      )

      val result = connector.getPaymentPlanDetails("test-dd-ref", "test-pp-ref").futureValue

      result.paymentPlanDetails.hodService shouldBe DirectDebitSource.MGD.toString
    }
  }

  "isDuplicatePaymentPlan" should {

    val currentDate = LocalDate.now()

    val duplicateCheckRequest: PaymentPlanDuplicateCheckRequest = PaymentPlanDuplicateCheckRequest(
      directDebitReference = "testRef",
      paymentPlanReference = "payment ref 123",
      planType             = "type 1",
      paymentService       = "CESA",
      paymentReference     = "payment ref",
      paymentAmount        = Some(120.00),
      totalLiability       = Some(120.00),
      paymentFrequency     = Some(1),
      paymentStartDate     = currentDate
    )

    "successfully return true when there is a duplicate Plan with 200 OK" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.toJson(DuplicateCheckResponse(true)).toString)
          )
      )

      val result: DuplicateCheckResponse = connector.isDuplicatePaymentPlan("testRef", duplicateCheckRequest).futureValue
      result shouldBe DuplicateCheckResponse(true)
    }

    "successfully return false when there is no duplicate Plan with 200 OK" in {
      stubFor(
        post(urlPathMatching("/national-direct-debit/direct-debits/testRef/duplicate-plan-check"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.toJson(DuplicateCheckResponse(false)).toString)
          )
      )

      val result: DuplicateCheckResponse = connector.isDuplicatePaymentPlan("testRef", duplicateCheckRequest).futureValue
      result shouldBe DuplicateCheckResponse(false)
    }

  }

}
