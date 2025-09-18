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
import config.FrontendAppConfig
import connectors.NationalDirectDebitConnector
import controllers.routes
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.{BudgetPaymentPlan, TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse}
import models.{DirectDebitSource, NddDetails, NddResponse, PaymentPlanType, YourBankDetailsWithAuddisStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, YourBankDetailsPage}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DirectDebitDetailsData

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

class NationalDirectDebitServiceSpec extends SpecBase
  with MockitoSugar
  with DirectDebitDetailsData {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
  }

  implicit val ec: ExecutionContext = global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: NationalDirectDebitConnector = mock[NationalDirectDebitConnector]
  val mockCache: DirectDebitCacheRepository = mock[DirectDebitCacheRepository]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockAuditService: AuditService = mock[AuditService]

  val service = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService)

  val testId = "id"
  val testSortCode = "123456"
  val testAccountNumber = "12345678"
  val testAccountHolderName = "Jon B Jones"
  implicit val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.LandingController.onPageLoad().url)

  val testBankDetailsAuddisTrue: YourBankDetailsWithAuddisStatus =
    YourBankDetailsWithAuddisStatus(accountHolderName = testAccountHolderName, sortCode = testSortCode, accountNumber = testAccountNumber, auddisStatus = true, false)

  val testPaymentPlanType: PaymentPlanType = VariablePaymentPlan
  val testDirectDebitSource: DirectDebitSource = MGD

  "NationalDirectDebitService" - {
    "retrieve" - {
      "should retrieve existing details from Cache first" in {
        when(mockCache.retrieveCache(any()))
          .thenReturn(Future.successful(nddResponse.directDebitList))

        val result = service.retrieveAllDirectDebits(testId).futureValue
        result mustEqual nddResponse
      }

      "should retrieve details from Connector if Cache is empty, and cache the response" in {
        when(mockCache.retrieveCache(any()))
          .thenReturn(Future.successful(Seq.empty[NddDetails]))
        when(mockConnector.retrieveDirectDebits()(any()))
          .thenReturn(Future.successful(nddResponse))
        when(mockCache.cacheResponse(any())(any()))
          .thenReturn(Future.successful(true))

        val result = service.retrieveAllDirectDebits(testId).futureValue
        result mustEqual nddResponse
      }

      "should be able to return no details from Connector or Cache is correctly empty" in {
        when(mockCache.retrieveCache(any()))
          .thenReturn(Future.successful(Seq.empty[NddDetails]))
        when(mockConnector.retrieveDirectDebits()(any()))
          .thenReturn(Future.successful(NddResponse(0, Seq())))
        when(mockCache.cacheResponse(any())(any()))
          .thenReturn(Future.successful(true))

        val result = service.retrieveAllDirectDebits(testId).futureValue
        result mustEqual NddResponse(0, Seq())
      }

      "must create an audit event when retrieving direct debits from the backend" in {
        when(mockCache.retrieveCache(any()))
          .thenReturn(Future.successful(Seq.empty[NddDetails]))
        when(mockConnector.retrieveDirectDebits()(any()))
          .thenReturn(Future.successful(nddResponse))
        when(mockCache.cacheResponse(any())(any()))
          .thenReturn(Future.successful(true))
        doNothing().when(mockAuditService).sendEvent(any())(any(), any(), any())

        val result = service.retrieveAllDirectDebits(testId).futureValue
        verify(mockAuditService).sendEvent(any())(any(), any(), any())
        result mustEqual nddResponse
      }
    }

    "getEarliestPaymentDate" - {
      "must successfully return the Earliest Payment Date" in {
        val expectedUserAnswers = emptyUserAnswers.set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate("2025-12-25")))

        val result = service.getEarliestPaymentDate(expectedUserAnswers).futureValue

        result mustBe EarliestPaymentDate("2025-12-25")
      }

      "fail when auddis status is not in user answers" in {
        val result = intercept[Exception](service.getEarliestPaymentDate(emptyUserAnswers).futureValue)

        result.getMessage must include("YourBankDetailsPage details missing from user answers")
      }

      "fail when the connector call fails" in {
        val expectedUserAnswers = emptyUserAnswers.set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.getEarliestPaymentDate(expectedUserAnswers).futureValue)

        result.getMessage must include("bang")
      }
    }

    "getEarliestPlanStartDate" - {
      "must successfully return the Earliest Payment Date" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType).success.value
          .set(DirectDebitSourcePage, testDirectDebitSource).success.value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate("2025-12-25")))

        val result = service.getEarliestPlanStartDate(expectedUserAnswers).futureValue

        result mustBe EarliestPaymentDate("2025-12-25")
      }

      "fail when auddis status is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType).success.value
          .set(DirectDebitSourcePage, testDirectDebitSource).success.value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("YourBankDetailsPage details missing from user answers")
      }
      "fail when payment plan type is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, testDirectDebitSource).success.value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("PaymentPlanTypePage details missing from user answers")
      }
      "fail when direct debit source is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType).success.value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("DirectDebitSourcePage details missing from user answers")
      }
      "fail when the connector call fails" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType).success.value
          .set(DirectDebitSourcePage, testDirectDebitSource).success.value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("bang")
      }
    }

    "calculateOffset using auddis status method" - {
      "successfully calculate the offset when auddis status is enabled" in {
        val auddisStatus = true

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)

        val expected = 5

        service.calculateOffset(auddisStatus) mustBe expected
      }
      "successfully calculate the offset when auddis status is not enabled" in {
        val auddisStatus = false
        val expectedVariableDelay = 8

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisNotEnabled).thenReturn(expectedVariableDelay)

        val expected = 10

        service.calculateOffset(auddisStatus) mustBe expected
      }
    }

    "calculateOffset using auddis status, payment plan type and direct debit source method " - {
      "successfully calculate the offset when source is machine games duty" in {
        val auddisStatus = true

        when(mockConfig.variableMgdFixedDelay).thenReturn(10)

        val expected = 10

        service.calculateOffset(auddisStatus, VariablePaymentPlan, MGD) mustBe expected
      }
      "successfully calculate the offset when auddis status is enabled and source is self assessment" in {
        val auddisStatus = true

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)

        val expected = 5

        service.calculateOffset(auddisStatus, BudgetPaymentPlan, SA) mustBe expected
      }
      "successfully calculate the offset when auddis status is not enabled and source is tax credits" in {
        val auddisStatus = false
        val expectedVariableDelay = 8

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisNotEnabled).thenReturn(expectedVariableDelay)

        val expected = 10

        service.calculateOffset(auddisStatus, BudgetPaymentPlan, SA) mustBe expected
      }
      "successfully calculate the offset when auddis status is enabled and source is tax credits" in {
        val auddisStatus = true

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)

        val expected = 5

        service.calculateOffset(auddisStatus, TaxCreditRepaymentPlan, TC) mustBe expected
      }
      "successfully calculate the offset when auddis status is not enabled and source is self assessment" in {
        val auddisStatus = false
        val expectedVariableDelay = 8

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisNotEnabled).thenReturn(expectedVariableDelay)

        val expected = 10

        service.calculateOffset(auddisStatus, TaxCreditRepaymentPlan, TC) mustBe expected
      }
    }

    "generateNewDdiReference" - {
      "must successfully return the DDI Reference Number" in {
        when(mockConnector.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRes")))

        val result = service.generateNewDdiReference("testRef").futureValue

        result mustBe GenerateDdiRefResponse("testRes")
      }

      "fail when the connector call fails" in {
        when(mockConnector.generateNewDdiReference(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.generateNewDdiReference("testRef").futureValue)

        result.getMessage must include("bang")
      }
    }

    "submitChrisData" - {

      val planStartDateDetails = models.PlanStartDateDetails(
        enteredDate = java.time.LocalDate.of(2025, 9, 1),
        earliestPlanStartDate = "2025-09-01"
      )

      val paymentDateDetails = models.PaymentDateDetails(
        enteredDate = java.time.LocalDate.of(2025, 9, 15),
        earliestPaymentDate = "2025-09-01"
      )

      val chrisSubmission = models.requests.ChrisSubmissionRequest(
        serviceType = DirectDebitSource.TC,
        paymentPlanType = PaymentPlanType.TaxCreditRepaymentPlan,
        paymentFrequency = Some(models.PaymentsFrequency.Monthly),
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
        bankDetailsAddress = models.responses.BankAddress(
          lines = Seq("line 1"),
          town = "Town",
          country = models.responses.Country("UK"),
          postCode = "NE5 2DH"
        ),
        ddiReferenceNo = "DDI123456789",
        paymentReference = "testReference",
        bankName = "Barclays",
        totalAmountDue = Some(BigDecimal(200)),
        paymentAmount = Some(BigDecimal(100)),
        regularPaymentAmount = Some(BigDecimal(90)),
        calculation = None
      )

      "must return true when CHRIS submission succeeds" in {
        when(mockConnector.submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))

        val result = service.submitChrisData(chrisSubmission).futureValue
        result mustBe true

        verify(mockConnector, atLeastOnce()).submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier])
      }

      "must return false when CHRIS submission fails with exception" in {
        when(mockConnector.submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("submission failed")))

        val result = service.submitChrisData(chrisSubmission).futureValue
        result mustBe false
        verify(mockConnector, atLeastOnce()).submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier])
      }
    }

  }

}
