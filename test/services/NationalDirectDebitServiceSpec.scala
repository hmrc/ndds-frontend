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
import models.responses.*
import models.{DirectDebitSource, NddDetails, NddResponse, PaymentPlanType, PaymentsFrequency, YourBankDetailsWithAuddisStatus}
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
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery, PaymentPlansCountQuery}
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DirectDebitDetailsData
import utils.Frequency.*

import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}

class NationalDirectDebitServiceSpec extends SpecBase with MockitoSugar with DirectDebitDetailsData {

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
  val currentClock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
  val service = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, currentClock)

  val testId = "id"
  val testSortCode = "123456"
  val testAccountNumber = "12345678"
  val testAccountHolderName = "Jon B Jones"
  implicit val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.LandingController.onPageLoad().url)

  val testBankDetailsAuddisTrue: YourBankDetailsWithAuddisStatus =
    YourBankDetailsWithAuddisStatus(accountHolderName = testAccountHolderName,
                                    sortCode          = testSortCode,
                                    accountNumber     = testAccountNumber,
                                    auddisStatus      = true,
                                    false
                                   )

  val testPaymentPlanType: PaymentPlanType = VariablePaymentPlan
  val testDirectDebitSource: DirectDebitSource = MGD

  val singlePlan = "singlePaymentPlan"
  val budgetPlan = "budgetPaymentPlan"

  "NationalDirectDebitService" - {
    "retrieveDirectDebits" - {
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
          .set(PaymentPlanTypePage, testPaymentPlanType)
          .success
          .value
          .set(DirectDebitSourcePage, testDirectDebitSource)
          .success
          .value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue)
          .success
          .value

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getFutureWorkingDays(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate("2025-12-25")))

        val result = service.getEarliestPlanStartDate(expectedUserAnswers).futureValue

        result mustBe EarliestPaymentDate("2025-12-25")
      }

      "fail when auddis status is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType)
          .success
          .value
          .set(DirectDebitSourcePage, testDirectDebitSource)
          .success
          .value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("YourBankDetailsPage details missing from user answers")
      }

      "fail when payment plan type is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(DirectDebitSourcePage, testDirectDebitSource)
          .success
          .value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue)
          .success
          .value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("PaymentPlanTypePage details missing from user answers")
      }

      "fail when direct debit source is not in user answers" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType)
          .success
          .value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue)
          .success
          .value

        val result = intercept[Exception](service.getEarliestPlanStartDate(expectedUserAnswers).futureValue)

        result.getMessage must include("DirectDebitSourcePage details missing from user answers")
      }

      "fail when the connector call fails" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, testPaymentPlanType)
          .success
          .value
          .set(DirectDebitSourcePage, testDirectDebitSource)
          .success
          .value
          .set(YourBankDetailsPage, testBankDetailsAuddisTrue)
          .success
          .value

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

    "retrieveDirectDebitPaymentPlans" - {
      "must successfully return the direct debit payment plans" in {
        val paymentPlanResponse = NddDDPaymentPlansResponse(bankSortCode      = "123456",
                                                            bankAccountNumber = "12345678",
                                                            bankAccountName   = "MyBankAcc",
                                                            auDdisFlag        = "01",
                                                            paymentPlanCount  = 2,
                                                            paymentPlanList   = Seq.empty
                                                           )

        when(mockConnector.retrieveDirectDebitPaymentPlans(any())(any()))
          .thenReturn(Future.successful(paymentPlanResponse))

        val result = service.retrieveDirectDebitPaymentPlans("testRef").futureValue

        result mustBe paymentPlanResponse
      }

      "fail when the connector call fails" in {
        when(mockConnector.retrieveDirectDebitPaymentPlans(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.retrieveDirectDebitPaymentPlans("testRef").futureValue)

        result.getMessage must include("bang")
      }
    }

    "amendPaymentPlanGuard" - {
      "must return true if single payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if single payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if budget payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return true if budget payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe true
      }

      "must return false if variable payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if variable payment for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if tax credit repayment payment for set up journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .success
          .value
        val result = service.amendPaymentPlanGuard(expectedUserAnswers)
        result mustBe false
      }

      "must return false if payment plan is empty for amend journey" in {
        val expectedUserAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, "")
          .success
          .value
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
        enteredDate           = java.time.LocalDate.of(2025, 9, 1),
        earliestPlanStartDate = "2025-09-01"
      )

      val paymentDateDetails = models.PaymentDateDetails(
        enteredDate         = java.time.LocalDate.of(2025, 9, 15),
        earliestPaymentDate = "2025-09-01"
      )

      val chrisSubmission = models.requests.ChrisSubmissionRequest(
        serviceType                = DirectDebitSource.TC,
        paymentPlanType            = PaymentPlanType.TaxCreditRepaymentPlan,
        paymentPlanReferenceNumber = None,
        paymentFrequency           = Some(models.PaymentsFrequency.Monthly.toString),
        yourBankDetailsWithAuddisStatus = YourBankDetailsWithAuddisStatus(
          accountHolderName = "Test",
          sortCode          = "123456",
          accountNumber     = "12345678",
          auddisStatus      = false,
          accountVerified   = false
        ),
        planStartDate             = Some(planStartDateDetails),
        planEndDate               = None,
        paymentDate               = Some(paymentDateDetails),
        yearEndAndMonth           = None,
        ddiReferenceNo            = "DDI123456789",
        paymentReference          = "testReference",
        totalAmountDue            = Some(BigDecimal(200)),
        paymentAmount             = Some(BigDecimal(100)),
        regularPaymentAmount      = Some(BigDecimal(90)),
        amendPaymentAmount        = None,
        calculation               = None,
        suspensionPeriodRangeDate = None
      )

      "must return true when CHRIS submission succeeds" in {
        when(mockConnector.submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))

        val result = service.submitChrisData(chrisSubmission).futureValue
        result mustBe true

        verify(mockConnector, atLeastOnce()).submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier])
      }
      val currentTime = LocalDateTime.now().withNano(0)
      val chrisAmendSubmission = models.requests.ChrisSubmissionRequest(
        serviceType                = DirectDebitSource.TC,
        paymentPlanType            = PaymentPlanType.TaxCreditRepaymentPlan,
        paymentPlanReferenceNumber = None,
        paymentFrequency           = Some(models.PaymentsFrequency.Monthly.toString),
        yourBankDetailsWithAuddisStatus = YourBankDetailsWithAuddisStatus(
          accountHolderName = "Test",
          sortCode          = "123456",
          accountNumber     = "12345678",
          auddisStatus      = true,
          accountVerified   = true
        ),
        planStartDate             = Some(planStartDateDetails),
        planEndDate               = Some(currentTime.toLocalDate.plusYears(1)),
        paymentDate               = Some(paymentDateDetails),
        yearEndAndMonth           = None,
        ddiReferenceNo            = "DDI123456789",
        paymentReference          = "testReference",
        totalAmountDue            = Some(BigDecimal(200)),
        paymentAmount             = Some(BigDecimal(100)),
        regularPaymentAmount      = Some(BigDecimal(90)),
        amendPaymentAmount        = None,
        calculation               = None,
        amendPlan                 = true,
        suspensionPeriodRangeDate = None
      )

      "must return true when CHRIS submission succeeds for amend" in {
        when(mockConnector.submitChrisData(any[models.requests.ChrisSubmissionRequest]())(any[HeaderCarrier]))
          .thenReturn(Future.successful(true))

        val result = service.submitChrisData(chrisAmendSubmission).futureValue
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

    "getPaymentPlanDetails" - {
      "must successfully return the payment plan detail" in {

        val planDetailsResponse = dummyPlanDetailResponse

        when(mockConnector.getPaymentPlanDetails(any(), any())(any()))
          .thenReturn(Future.successful(planDetailsResponse))

        val result = service.getPaymentPlanDetails("test-ddRef", "test-pp-ref").futureValue

        result mustBe planDetailsResponse
      }

      "fail when the connector call fails" in {
        when(mockConnector.getPaymentPlanDetails(any(), any())(any()))
          .thenReturn(Future.failed(new Exception("error")))

        val result = intercept[Exception](service.getPaymentPlanDetails("test-ddRef", "test-pp-ref").futureValue)

        result.getMessage must include("error")
      }
    }

    "lockPaymentPlan" - {
      "must successfully return Ok" in {
        when(mockConnector.lockPaymentPlan(any(), any())(any()))
          .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

        val result = service.lockPaymentPlan("test-dd-ref", "test-pp-ref").futureValue

        result mustBe AmendLockResponse(lockSuccessful = true)
      }

      "fail when the connector call fails" in {
        when(mockConnector.lockPaymentPlan(any(), any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](service.lockPaymentPlan("test-dd-ref", "test-pp-ref").futureValue)

        result.getMessage must include("bang")
      }
    }

    "isDuplicatePaymentPlan" - {

      "return true when count is more than 1 payment plan and it is single payment plan so connector returns true" in {
        val mockSinglePaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(
              planType                  = PaymentPlanType.SinglePaymentPlan.toString,
              hodService                = DirectDebitSource.SA.toString,
              scheduledPaymentFrequency = Some(PaymentsFrequency.FortNightly.toString),
              totalLiability            = Some(780.0)
            )
          )

        val currentDate = LocalDate.now()

        val userAnswersSingle = emptyUserAnswers
          .set(DirectDebitReferenceQuery, "default ref 1")
          .success
          .value
          .set(PaymentPlanReferenceQuery, "payment ref 1")
          .success
          .value
          .set(PaymentPlanDetailsQuery, mockSinglePaymentPlanDetailResponse)
          .success
          .value
          .set(PaymentPlansCountQuery, 4)
          .success
          .value
          .set(AmendPlanStartDatePage, currentDate)
          .success
          .value

        when(mockConnector.isDuplicatePaymentPlan(any(), any())(any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

        val result: DuplicateCheckResponse = service.isDuplicatePaymentPlan(userAnswersSingle).futureValue

        result shouldBe DuplicateCheckResponse(true)
      }

      "return false count is 1 payment plan and single payment plan so no call to connector returns false" in {
        val userAnswers = emptyUserAnswers
          .set(DirectDebitReferenceQuery, "default ref 1")
          .success
          .value
          .set(PaymentPlansCountQuery, 1)
          .success
          .value

        val result: DuplicateCheckResponse = service.isDuplicatePaymentPlan(userAnswers).futureValue

        result shouldBe DuplicateCheckResponse(false)
      }

      "return true when count is more than 1 payment plan and it is budget payment plan so connector returns true" in {
        val mockBudgetPaymentPlanDetailResponse =
          dummyPlanDetailResponse.copy(paymentPlanDetails =
            dummyPlanDetailResponse.paymentPlanDetails.copy(
              planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
              hodService                = DirectDebitSource.SA.toString,
              scheduledPaymentFrequency = Some(PaymentsFrequency.FortNightly.toString),
              totalLiability            = Some(780.0)
            )
          )

        val userAnswersBudget = emptyUserAnswers
          .set(DirectDebitReferenceQuery, "default ref 1")
          .success
          .value
          .set(PaymentPlanReferenceQuery, "payment ref 1")
          .success
          .value
          .set(PaymentPlanDetailsQuery, mockBudgetPaymentPlanDetailResponse)
          .success
          .value
          .set(PaymentPlansCountQuery, 4)
          .success
          .value

        when(mockConnector.isDuplicatePaymentPlan(any(), any())(any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

        val result: DuplicateCheckResponse = service.isDuplicatePaymentPlan(userAnswersBudget).futureValue

        result shouldBe DuplicateCheckResponse(true)
      }

      "return false when count is 1 payment plan so no call to the connector returns false" in {
        val userAnswersBudget = emptyUserAnswers
          .set(DirectDebitReferenceQuery, "default ref 1")
          .success
          .value
          .set(PaymentPlansCountQuery, 1)
          .success
          .value

        val result: DuplicateCheckResponse = service.isDuplicatePaymentPlan(userAnswersBudget).futureValue

        result shouldBe DuplicateCheckResponse(false)
      }
    }

    "isAdvanceNoticePresent" - {
      "return true when AdvanceNoticeResponse is present" in {
        val response = new AdvanceNoticeResponse(Some("100.00"), Some("2025-11-30"))
        when(mockConnector.isAdvanceNoticePresent(any(), any())(any()))
          .thenReturn(Future.successful(Some(response)))

        service.isAdvanceNoticePresent("ddRef", "planRef").map { result =>
          result shouldBe true
        }
      }

      "return false when no AdvanceNoticeResponse is present" in {
        when(mockConnector.isAdvanceNoticePresent(any(), any())(any()))
          .thenReturn(Future.successful(None))

        service.isAdvanceNoticePresent("ddRef", "planRef").map { result =>
          result shouldBe false
        }
      }

      "return false when the connector call fails" in {
        when(mockConnector.isAdvanceNoticePresent(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        service.isAdvanceNoticePresent("ddRef", "planRef").map { result =>
          result shouldBe false
        }
      }
    }
  }

  "calculateNextPaymentDate" - {

    "when planEndDate is None then potentialNextPaymentDate should return None" in {
      val today = LocalDate.now()
      val startDate = today.plusDays(1) // start date is tomorrow
      val planEndDate = None

      val result = service.calculateNextPaymentDate(startDate, planEndDate, Monthly).futureValue

      result.potentialNextPaymentDate mustBe None
      result.nextPaymentDateValid mustBe true
    }

    "when planStartDate is beyond three working days then potentialNextPaymentDate should return startDate" in {
      val today = LocalDate.now()
      val startDate = today.plusDays(10)
      val planEndDate = today.plusDays(20)

      when(mockConnector.getFutureWorkingDays(any())(any()))
        .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

      val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

      result.potentialNextPaymentDate mustBe Some(startDate)
      result.nextPaymentDateValid mustBe true
    }

    "when planStartDate is within three working days then potentialNextPaymentDate is after planEndDate nextPaymentDateValid must be false" in {
      val today = LocalDate.now()
      val startDate = today.plusDays(10)
      val planEndDate = today.plusDays(20)

      when(mockConnector.getFutureWorkingDays(any())(any()))
        .thenReturn(Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)))

      val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

      result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(1))
      result.nextPaymentDateValid mustBe false
    }

    ".forWeeklyFrequency" - {

      ".frequency is Weekly" - {
        "when planStartDate is a past date but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusDays(20)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Weekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(2))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is a past date but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusDays(20)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(2).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Weekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(3))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(3)
          val planEndDate = today.plusDays(20)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Weekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(1))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(3)
          val planEndDate = today.plusDays(20)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(1).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Weekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(2))
          result.nextPaymentDateValid mustBe true
        }
      }

      ".frequency is Fortnightly" - {
        "when planStartDate is a past date but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Fortnightly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(2))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is a past date but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(2).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Fortnightly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(4))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(2)
          val planEndDate = today.plusDays(20)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Fortnightly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(2))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(2)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(2).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Fortnightly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(4))
          result.nextPaymentDateValid mustBe true
        }
      }

      ".frequency is FourWeekly" - {
        "when planStartDate is a past date but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), FourWeekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(4))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is a past date but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.minusDays(10)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(4).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), FourWeekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(8))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is not within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(2)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), FourWeekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(4))
          result.nextPaymentDateValid mustBe true
        }

        "when planStartDate is prior to next three working days but potentialNextPaymentDate is within 3 working days" in {
          val today = LocalDate.now()
          val startDate = today.plusDays(2)
          val planEndDate = today.plusMonths(3)

          when(mockConnector.getFutureWorkingDays(any())(any()))
            .thenReturn(
              Future.successful(EarliestPaymentDate(today.plusDays(3).toString)),
              Future.successful(EarliestPaymentDate(startDate.plusWeeks(4).plusDays(3).toString))
            )

          val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), FourWeekly).futureValue

          result.potentialNextPaymentDate mustBe Some(startDate.plusWeeks(8))
          result.nextPaymentDateValid mustBe true
        }
      }
    }

    ".forMonthlyFrequency" - {

      ".frequency is Monthly" - {
        "when planStartDate is within 3 working days" - {

          "but potentialNextPaymentDate is not within 3 working days" in {
            val today = LocalDate.now()
            val startDate = today.plusDays(2)
            val planEndDate = today.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(1))
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 1, 31)
            val startDate = today
            val planEndDate = today.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedPotentialDate = today.plusMonths(2).withDayOfMonth(1)

            result.potentialNextPaymentDate mustBe Some(expectedPotentialDate)
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is not a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 4, 30)
            val startDate = today
            val planEndDate = today.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            result.potentialNextPaymentDate mustBe Some(today.plusMonths(1))
            result.nextPaymentDateValid mustBe true
          }
        }

        "when planStartDate is a past date" - {

          "but planStartDate is same calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedDate = LocalDate.of(2025, 11, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 1, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedDate = LocalDate.of(2025, 11, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-01-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 1, 31)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedDate = LocalDate.of(2025, 3, 1)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is not a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-04-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 4, 29)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedDate = LocalDate.of(2025, 5, 29)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is more than one calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 2, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(3)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Monthly).futureValue

            val expectedDate = LocalDate.of(2025, 11, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }
        }
      }

      ".frequency is Quarterly" - {
        "when planStartDate is within 3 working days" - {

          "but potentialNextPaymentDate is not within 3 working days" in {
            val today = LocalDate.now()
            val startDate = today.plusDays(2)
            val planEndDate = today.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(3))
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 1, 31)
            val startDate = today
            val planEndDate = today.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedPotentialDate = today.plusMonths(4).withDayOfMonth(1)

            result.potentialNextPaymentDate mustBe Some(expectedPotentialDate)
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is not a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 4, 30)
            val startDate = today
            val planEndDate = today.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            result.potentialNextPaymentDate mustBe Some(today.plusMonths(3))
            result.nextPaymentDateValid mustBe true
          }
        }

        "when planStartDate is a past date" - {

          "but planStartDate is same calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 12, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 1, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 12, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-03-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 3, 31)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 7, 1)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is not a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-02-28T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 2, 28)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 5, 28)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is more than one calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 2, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusMonths(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 12, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is more than one calendar year and potentialNextPaymentDate is within 3 working days and next month is not a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-10-29T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)
            val startDate = LocalDate.of(2020, 3, 29)
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Quarterly).futureValue

            val expectedDate = LocalDate.of(2025, 12, 29)

            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }
        }
      }

      ".frequency is SixMonthly" - {
        "when planStartDate is within 3 working days" - {

          "but potentialNextPaymentDate is not within 3 working days" in {
            val today = LocalDate.now()
            val startDate = today.plusDays(2)
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(6))
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 3, 31)
            val startDate = today
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedPotentialDate = today.plusMonths(7).withDayOfMonth(1)

            result.potentialNextPaymentDate mustBe Some(expectedPotentialDate)
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is not a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 4, 30)
            val startDate = today
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            result.potentialNextPaymentDate mustBe Some(today.plusMonths(6))
            result.nextPaymentDateValid mustBe true
          }
        }

        "when planStartDate is a past date" - {

          "but planStartDate is same calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedDate = LocalDate.of(2026, 3, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 1, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedDate = LocalDate.of(2026, 3, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-05-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 5, 31)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedDate = LocalDate.of(2025, 12, 1)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is not a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-04-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 4, 29)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedDate = LocalDate.of(2025, 10, 29)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is more than one calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 2, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), SixMonthly).futureValue

            val expectedDate = LocalDate.of(2026, 3, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }
        }
      }

      ".frequency is Annually" - {
        "when planStartDate is within 3 working days" - {

          "but potentialNextPaymentDate is not within 3 working days" in {
            val today = LocalDate.now()
            val startDate = today.plusDays(2)
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(12))
            result.nextPaymentDateValid mustBe true
          }

          "but potentialNextPaymentDate is not within 3 working days and next month is not a shorter month" in {
            val today = LocalDate.of(LocalDate.now().getYear + 1, 1, 31)
            val startDate = today
            val planEndDate = today.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            result.potentialNextPaymentDate mustBe Some(today.plusMonths(12))
            result.nextPaymentDateValid mustBe true
          }
        }

        "when planStartDate is a past date" - {

          "but planStartDate is same calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            result.potentialNextPaymentDate mustBe Some(startDate.plusMonths(12))
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 1, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            val expectedDate = LocalDate.of(2026, 3, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is more than one calendar year and potentialNextPaymentDate is not within 3 working days" in {
            val actualToday = LocalDate.now()
            val today = LocalDate.of(LocalDate.now().getYear - 2, 3, 15)
            val startDate = today
            val planEndDate = actualToday.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(Future.successful(EarliestPaymentDate(today.plusDays(3).toString)))

            val result = service.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            val expectedDate = LocalDate.of(2026, 3, 15)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }

          "but planStartDate is last calendar year and potentialNextPaymentDate is within 3 working days and next month is not a shorter month" in {
            val fixedClock = Clock.fixed(Instant.parse("2025-04-30T00:00:00Z"), ZoneId.of("UTC"))
            val nddService = new NationalDirectDebitService(mockConnector, mockCache, mockConfig, mockAuditService, fixedClock)

            val today = LocalDate.now(fixedClock)

            val startDate = LocalDate.of(2024, 4, 29)
            val planEndDate = startDate.plusYears(6)

            when(mockConnector.getFutureWorkingDays(any())(any()))
              .thenReturn(
                Future.successful(EarliestPaymentDate(startDate.plusDays(3).toString)),
                Future.successful(EarliestPaymentDate(today.plusDays(3).toString))
              )

            val result = nddService.calculateNextPaymentDate(startDate, Some(planEndDate), Annually).futureValue

            val expectedDate = LocalDate.of(2026, 4, 29)
            result.potentialNextPaymentDate mustBe Some(expectedDate)
            result.nextPaymentDateValid mustBe true
          }
        }
      }
    }
  }

  "isPaymentPlanCancellable" - {
    "must return true when plan type is SinglePaymentPlan" in {
      val userAnswers =
        emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString).success.value

      service.isPaymentPlanCancellable(userAnswers) mustBe true
    }

    "must return true when plan type is BudgetPaymentPlan" in {
      val userAnswers =
        emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString).success.value

      service.isPaymentPlanCancellable(userAnswers) mustBe true
    }

    "must return true when plan type is VariablePaymentPlan" in {
      val userAnswers =
        emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString).success.value

      service.isPaymentPlanCancellable(userAnswers) mustBe true
    }

    "must return true when plan type is TaxCreditRepaymentPlan" in {
      val userAnswers =
        emptyUserAnswers.set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString).success.value

      service.isPaymentPlanCancellable(userAnswers) mustBe false
    }
  }

  "earliestSuspendStartDate" - {
    "must successfully return a date that is 3 working days ahead of today" in {
      val today = LocalDate.now()
      val expectedDate = today.plusDays(5)
      when(mockConnector.getFutureWorkingDays(any())(any()))
        .thenReturn(Future.successful(EarliestPaymentDate(expectedDate.toString)))

      val result = service.earliestSuspendStartDate()(hc).futureValue

      result mustBe expectedDate
    }

    "must successfully return a date that is N working days ahead of today when offset is provided" in {
      val today = LocalDate.now()
      val expectedDate = today.plusDays(10)

      when(mockConnector.getFutureWorkingDays(any())(any()))
        .thenReturn(Future.successful(EarliestPaymentDate(expectedDate.toString)))

      val result = service.earliestSuspendStartDate(workingDaysOffset = 7)(hc).futureValue

      result mustBe expectedDate
    }

    "must fail when connector call fails" in {
      when(mockConnector.getFutureWorkingDays(any())(any()))
        .thenReturn(Future.failed(new Exception("service unavailable")))

      val exception = intercept[Exception](service.earliestSuspendStartDate()(hc).futureValue)
      exception.getMessage must include("service unavailable")
    }
  }

  "suspendPaymentPlanGuard" - {

    "must return false if single payment for suspend journey" in {
      val expectedUserAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value
      val result = service.suspendPaymentPlanGuard(expectedUserAnswers)
      result mustBe false
    }

    "must return true if budget payment for suspend journey" in {
      val expectedUserAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
      val result = service.suspendPaymentPlanGuard(expectedUserAnswers)
      result mustBe true
    }

    "must return false if variable payment for suspend journey" in {
      val expectedUserAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString)
        .success
        .value
      val result = service.suspendPaymentPlanGuard(expectedUserAnswers)
      result mustBe false
    }

    "must return false if tax credit repayment payment for suspend journey" in {
      val expectedUserAnswers = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan.toString)
        .success
        .value
      val result = service.suspendPaymentPlanGuard(expectedUserAnswers)
      result mustBe false
    }

    "must return false if payment plan is not set" in {
      val result = service.suspendPaymentPlanGuard(emptyUserAnswers)
      result mustBe false
    }
  }
}
