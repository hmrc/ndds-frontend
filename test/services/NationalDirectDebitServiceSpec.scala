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
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import queries.PaymentPlanTypeQuery
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DirectDebitDetailsData
import models.PlanStartDateDetails

import java.time.LocalDate
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
  
  val singlePlan = "singlePaymentPlan"
  val budgetPlan = "budgetPaymentPlan"

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
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate("2025-12-25")))

        val result = service.calculateFutureWorkingDays(expectedUserAnswers).futureValue

        result mustBe EarliestPaymentDate("2025-12-25")
      }

      "fail when auddis status is not in user answers" in {
        val result = intercept[Exception](service.calculateFutureWorkingDays(emptyUserAnswers).futureValue)

        result.getMessage must include("YourBankDetailsPage details missing from user answers")
      }

      "fail when the connector call fails" in {
        val expectedUserAnswers = emptyUserAnswers.set(YourBankDetailsPage, testBankDetailsAuddisTrue).success.value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.calculateFutureWorkingDays(expectedUserAnswers).futureValue)

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
        when(mockConnector.getFutureWorkingDays(any())(any()))
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
        when(mockConnector.getFutureWorkingDays(any())(any()))
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

    "amendPaymentPlanGuard" - {
      "must return true if single payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if single payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, PaymentPlanType.SinglePaymentPlan.toString).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if budget payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if budget payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, PaymentPlanType.BudgetPaymentPlan.toString).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return false if variable payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if variable payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, PaymentPlanType.VariablePaymentPlan.toString).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if tax credit repayment payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan).success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if payment plan is empty for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypeQuery, "").success.value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }
    }

    "isTwoDaysPriorPaymentDate" - {
      "must return true future working days" in {
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate(LocalDate.now().toString)))

        val result = service.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(2)).futureValue

        result mustBe true
      }

      "must return false future working days" in {
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate(LocalDate.now().toString)))

        val result = service.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(0)).futureValue

        result mustBe false
      }
    }

    "isThreeDaysPriorPlanEndDate" - {
      "must return true future working days" in {
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate(LocalDate.now().toString)))

        val result = service.isThreeDaysPriorPlanEndDate(LocalDate.now().plusDays(3)).futureValue

        result mustBe true
      }

      "must return false future working days" in {
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate(LocalDate.now().toString)))

        val result = service.isThreeDaysPriorPlanEndDate(LocalDate.now().plusDays(0)).futureValue

        result mustBe false
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
  
  "amendmentMade" - {

    val today = LocalDate.now()
    val start0 = today.plusDays(1)
    val start1 = today.plusDays(2)
    val end0 = today.plusMonths(1)
    val end1 = today.plusMonths(2)
    val earliestStart = today.plusDays(3).toString

    "return true when amount changed for a single plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
        .set(PlanStartDatePage, PlanStartDateDetails(start0, earliestStart)).success.value

      service.amendmentMade(userAnswers) shouldBe true
    }

    "return true when date changed for a single plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(AmendPlanStartDatePage, start0).success.value
        .set(NewAmendPlanStartDatePage, start1).success.value

      service.amendmentMade(userAnswers) shouldBe true
    }

    "return true when both amount and date are changed for single plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
        .set(AmendPlanStartDatePage, start0).success.value
        .set(AmendPlanStartDatePage, start1).success.value

      service.amendmentMade(userAnswers) shouldBe true
    }

    "return false when neither amount nor date are changed for a single plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(AmendPlanStartDatePage, start0).success.value
        .set(NewAmendPlanStartDatePage, start0).success.value

      service.amendmentMade(userAnswers) shouldBe false
    }

    "return true when amount changed for budget plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, budgetPlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
        .set(PlanEndDatePage, end0).success.value
      
      service.amendmentMade(userAnswers) shouldBe true
    }
    
    "return true when date changed for budget plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, budgetPlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(AmendPlanEndDatePage, end0).success.value
        .set(NewAmendPlanEndDatePage, end1).success.value
      
      service.amendmentMade(userAnswers) shouldBe true
    }
    
    "return true when both amount and date are changed for a budget plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, budgetPlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
        .set(AmendPlanEndDatePage, end0).success.value
        .set(NewAmendPlanEndDatePage, end1).success.value

      service.amendmentMade(userAnswers) shouldBe true
    }
    
    "return false when neither amount nor date are changed for a budget plan" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, budgetPlan).success.value
        .set(AmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(100.00)).success.value
        .set(AmendPlanEndDatePage, end0).success.value
        .set(NewAmendPlanEndDatePage, end0).success.value

      service.amendmentMade(userAnswers) shouldBe false
    }
    
    "amountChanged returns true when existing amount is missing" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .remove(AmendPaymentAmountPage).success.value
        .set(NewAmendPaymentAmountPage, BigDecimal(120.00)).success.value
      service.amendmentMade(userAnswers) shouldBe true
    }

    "startDateChanged returns true when existing amount is missing" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, singlePlan).success.value
        .remove(AmendPlanStartDatePage).success.value
        .set(NewAmendPlanStartDatePage, start1).success.value
      service.amendmentMade(userAnswers) shouldBe true
    }

    "endDateChanged returns true when existing amount is missing" in {
      val userAnswers = emptyUserAnswers
        .set(PaymentPlanTypeQuery, budgetPlan).success.value
        .remove(AmendPlanEndDatePage).success.value
        .set(NewAmendPlanEndDatePage, start1).success.value
      service.amendmentMade(userAnswers) shouldBe true
    }
    
  }
  
}
