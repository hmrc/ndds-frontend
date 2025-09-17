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
import models.requests.WorkingDaysOffsetRequest
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse}
import models.{DirectDebitSource, NddDetails, NddResponse, PaymentPlanType, YourBankDetailsWithAuddisStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, reset, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, YourBankDetailsPage}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DirectDebitDetailsData

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
    YourBankDetailsWithAuddisStatus(
      accountHolderName = testAccountHolderName,
      sortCode = testSortCode,
      accountNumber = testAccountNumber,
      auddisStatus = true, false)

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

    "paymentDateWithinTwoWorkingDaysFlag" - {
      "should return false when the plan is not a SinglePayment and not call the connector" in {
        val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value
        val result = service.paymentDateWithinTwoWorkingDaysFlag(LocalDate.now.plusDays(1), ua)

        whenReady(result) (_ shouldBe false)
      }

      "should call the connector and return true if it is a SinglePayment and the payment date is within 2 working days" in {
        val today = LocalDate.now()
        val twoWorkingDaysFromNow = today.plusDays(2)
        val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.SinglePayment).success.value

        when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(EarliestPaymentDate(date = twoWorkingDaysFromNow.toString)))

        val paymentDate = twoWorkingDaysFromNow
        val result = service.paymentDateWithinTwoWorkingDaysFlag(paymentDate, ua)

        whenReady(result) (_ shouldBe true)
      }

      "should return false when paymentDate is after the connectors' two-working-days date" in {
        val today = LocalDate.now()
        val twoWorkingDaysFromNow = today.plusDays(2)
        val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.SinglePayment).success.value

        when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(EarliestPaymentDate(date = twoWorkingDaysFromNow.toString)))

        val paymentDate = twoWorkingDaysFromNow.plusDays(1)
        val result = service.paymentDateWithinTwoWorkingDaysFlag(paymentDate, ua)

        whenReady(result) (_ shouldBe false)
      }

      "should recover to false when the connector call fails" in {
        val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.SinglePayment).success.value
        
        when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("fail")))
        
        val result = service.paymentDateWithinTwoWorkingDaysFlag(LocalDate.now.plusDays(1), ua)
        
        whenReady(result) (_ shouldBe false)
      }
    }
  }
  
  "planEndWithinThreeWorkingDaysFlag" - {
    "should return false when the plan is not a BudgetPayment and not call the connector" in {
      val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan).success.value
      val result = service.planEndWithinThreeWorkingDaysFlag(Some(LocalDate.now.plusDays(1)), ua)

      whenReady(result)(_ shouldBe false)
    }

    "should call the connector and return true if it is a BudgetPaymentPlan and the payment date is within 3 working days" in {
      val today = LocalDate.now()
      val threeWorkingDaysFromNow = today.plusDays(3)
      val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value

      when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(EarliestPaymentDate(date = threeWorkingDaysFromNow.toString)))

      val paymentDate = Some(threeWorkingDaysFromNow)
      val result = service.planEndWithinThreeWorkingDaysFlag(paymentDate, ua)

      whenReady(result)(_ shouldBe true)
    }

    "should return false when paymentDate is after the connectors' three-working-days date" in {
      val today = LocalDate.now()
      val threeWorkingDaysFromNow = today.plusDays(3)
      val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value

      when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(EarliestPaymentDate(date = threeWorkingDaysFromNow.toString)))

      val paymentDate = Some(threeWorkingDaysFromNow.plusDays(1))
      val result = service.planEndWithinThreeWorkingDaysFlag(paymentDate, ua)

      whenReady(result)(_ shouldBe false)
    }

    "should recover to false when the connector call fails" in {
      val ua = emptyUserAnswers.set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value

      when(mockConnector.getEarliestPaymentDate(any[WorkingDaysOffsetRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("fail")))

      val result = service.planEndWithinThreeWorkingDaysFlag(Some(LocalDate.now.plusDays(1)), ua)

      whenReady(result)(_ shouldBe false)
    }
  }
  
  "canAmendPaymentPlan" - {
    
    "return true if it is a budget payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .success.value

      service.canAmendPaymentPlan(ua) mustBe true
    }

    "return true if it is a single payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.SinglePayment)
        .success.value
      
      service.canAmendPaymentPlan(ua) mustBe true
    }

    "return false if it is not a single payment plan or budget payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
        .success.value
      
      service.canAmendPaymentPlan(ua) mustBe false
    }
    
    "return false when plan type is missing" in {
      service.canAmendPaymentPlan(emptyUserAnswers) mustBe false
    }
  }
  
  "canCancelPaymentPLan" - {
    "return true if it is a budget payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .success.value

      service.canCancelPaymentPlan(ua) mustBe true
    }

    "return true if it is a single payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.SinglePayment)
        .success.value

      service.canCancelPaymentPlan(ua) mustBe true
    }

    "return true if it is a variable payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
        .success.value

      service.canCancelPaymentPlan(ua) mustBe true
    }

    "return false if it is not a single payment plan or budget payment plan" in {
      val ua = emptyUserAnswers
        .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .success.value

      service.canCancelPaymentPlan(ua) mustBe false
    }

    "return false when plan type is missing" in {
      service.canAmendPaymentPlan(emptyUserAnswers) mustBe false
    }
  }
}