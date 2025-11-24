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

package controllers

import base.SpecBase
import models.responses.{BankAddress, Country, DuplicateCheckResponse, GenerateDdiRefResponse}
import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ExistingDirectDebitIdentifierQuery
import repositories.DirectDebitCacheRepository
import services.NationalDirectDebitService
import utils.MacGenerator
import viewmodels.checkAnswers.YourBankDetailsNameSummary.nddResponse
import viewmodels.govuk.SummaryListFluency

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val fixedDate = LocalDate.of(2025, 7, 19)
  private val paymentDateDetails: PaymentDateDetails = PaymentDateDetails(fixedDate, "2025-7-19")
  private val planStartDateDetails: PlanStartDateDetails = PlanStartDateDetails(fixedDate, "2025-7-19")
  private val endDate = LocalDate.of(2027, 7, 25)
  private val yearEndAndMonthDate = YearEndAndMonth(2025, 4)
  private val mockNddService: NationalDirectDebitService = mock[NationalDirectDebitService]
  private val mockMacGenerator: MacGenerator = mock[MacGenerator]

  "Check Your Answers Controller" - {
    val userAnswer = emptyUserAnswers
      .setOrException(DirectDebitSourcePage, DirectDebitSource.CT)
      .setOrException(PaymentReferencePage, "1234567")
      .setOrException(PaymentAmountPage, 123.01)
      .setOrException(PaymentDatePage, paymentDateDetails)

    // GET tests
    "must return OK and the correct view if CT selected for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include(
          "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        )
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Accept and continue")
      }
    }

    "must redirect to not found page if user click browser back button from confirmation page" in {
      val updatedUserAnswer = userAnswer
        .setOrException(CreateConfirmationPage, true)

      val application = applicationBuilder(userAnswers = Some(updatedUserAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.BackSubmissionController.onPageLoad().url

      }
    }

    "must return OK and the correct view if MGD selected and type is single for a GET" in {
      val userAnswer = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.MGD)
        .setOrException(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentAmountPage, 123.01)
        .setOrException(PaymentDatePage, paymentDateDetails)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include(
          "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        )
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Accept and continue")
      }
    }

    "must return OK and the correct view if PAYE selected for a GET" in {
      val userAnswer = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.PAYE)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentAmountPage, 123.01)
        .setOrException(PaymentDatePage, paymentDateDetails)
        .setOrException(YearEndAndMonthPage, yearEndAndMonthDate)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include(
          "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        )
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Year end and month")
        contentAsString(result) must include("2025 04")
        contentAsString(result) must include("Accept and continue")
      }
    }

    "must return OK and the correct view if SA selected and plan type is budget for a GET" in {
      val userAnswer = emptyUserAnswers
        .setOrException(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentsFrequencyPage, PaymentsFrequency.Monthly)
        .setOrException(RegularPaymentAmountPage, 120)
        .setOrException(PlanStartDatePage, planStartDateDetails)
        .setOrException(PlanEndDatePage, endDate)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include(
          "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        )
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Frequency of payments")
        contentAsString(result) must include("Monthly")
        contentAsString(result) must include("Regular payment amount")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("25 Jul 2027")
        contentAsString(result) must include("£120")
        contentAsString(result) must include("Accept and continue")
      }
    }

    "must return OK and the correct view if TC selected and type is repayment for a GET" in {
      val userAnswer = emptyUserAnswers
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(TotalAmountDuePage, 4533)
        .setOrException(PlanStartDatePage, planStartDateDetails)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include(
          "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        )
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("1234567")
        contentAsString(result) must include("Total amount due")
        contentAsString(result) must include("4,533")
        contentAsString(result) must include("Plan start date")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("Monthly payment amount")
        contentAsString(result) must include("£377.75")
        contentAsString(result) must include("Final payment date")
        contentAsString(result) must include("19 Jun 2026")
        contentAsString(result) must include("Final payment amount")
        contentAsString(result) must include("£377.75")
        contentAsString(result) must include("Accept and continue")
      }
    }

    "must not show Plan End Date when AddPaymentPlanEndDatePage is false (NO)" in {
      val userAnswer = emptyUserAnswers
        .setOrException(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentsFrequencyPage, PaymentsFrequency.Monthly)
        .setOrException(RegularPaymentAmountPage, 120)
        .setOrException(PlanStartDatePage, planStartDateDetails)
        .setOrException(AddPaymentPlanEndDatePage, false)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must not include "25 Jul 2027"
        contentAsString(result) must include("Add plan end date")
        contentAsString(result) must not include "Plan End Date"
      }
    }

    "must show Plan End Date when AddPaymentPlanEndDatePage is true (YES) and PlanEndDatePage has a value" in {
      val userAnswer = emptyUserAnswers
        .setOrException(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentsFrequencyPage, PaymentsFrequency.Monthly)
        .setOrException(RegularPaymentAmountPage, 120)
        .setOrException(PlanStartDatePage, planStartDateDetails)
        .setOrException(PlanEndDatePage, endDate)
        .setOrException(AddPaymentPlanEndDatePage, true)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("25 Jul 2027")
        contentAsString(result) must include("Plan end date")
        contentAsString(result) must include("Add plan end date")
      }
    }

    "must show Plan End Date when AddPaymentPlanEndDatePage is not set and PlanEndDatePage has a value (backwards compatibility)" in {
      val userAnswer = emptyUserAnswers
        .setOrException(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentsFrequencyPage, PaymentsFrequency.Monthly)
        .setOrException(RegularPaymentAmountPage, 120)
        .setOrException(PlanStartDatePage, planStartDateDetails)
        .setOrException(PlanEndDatePage, endDate)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your payment plan details")
        contentAsString(result) must include("19 Jul 2025")
        contentAsString(result) must include("25 Jul 2027")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    // POST tests for missing fields, failed submissions, and successful submissions
    "must redirect to Journey Recovery for a POST if PaymentReferencePage data is missing" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
        .setOrException(BankDetailsBankNamePage, "Barclays")
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .setOrException(pages.MacValuePage, "valid-mac")

      when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(DuplicateCheckResponse(false)))

      when(mockNddService.generateNewDdiReference(any())(any()))
        .thenReturn(Future.successful(GenerateDdiRefResponse("fakeRef")))

      when(
        mockMacGenerator.generateMac(
          any[String],
          any[String],
          any[String],
          any[Seq[String]],
          any[Option[String]],
          any[Option[String]],
          any[String],
          any[String]
        )
      ).thenReturn("valid-mac")

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService),
          bind[MacGenerator].toInstance(mockMacGenerator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = intercept[Exception](route(application, request).value.futureValue)
        result.getMessage must include("Missing details: paymentReference")
      }
    }

    "must redirect to Journey Recovery for a POST if DirectDebitSource data is missing" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
        .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
        .setOrException(BankDetailsBankNamePage, "Barclays")
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .setOrException(PaymentReferencePage, "testRef")
        .setOrException(pages.MacValuePage, "valid-mac")

      when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(DuplicateCheckResponse(false)))

      when(
        mockMacGenerator.generateMac(
          any[String],
          any[String],
          any[String],
          any[Seq[String]],
          any[Option[String]],
          any[Option[String]],
          any[String],
          any[String]
        )
      ).thenReturn("valid-mac")

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService),
          bind[MacGenerator].toInstance(mockMacGenerator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val resultFuture: Future[play.api.mvc.Result] = route(application, request).get
        val exception = intercept[Exception] {
          await(resultFuture)
        }
        exception.getMessage must include("Missing details: directDebitSource")
      }
    }

    "must redirect to Journey Recovery for a POST if Ddi reference number is not obtained successfully" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(PaymentReferencePage, "testReference")
        .setOrException(pages.MacValuePage, "valid-mac")

      when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(DuplicateCheckResponse(false)))

      when(mockNddService.generateNewDdiReference(any())(any()))
        .thenReturn(Future.failed(new Exception("bang")))
      when(mockNddService.submitChrisData(any())(any()))
        .thenReturn(Future.successful(true))

      when(
        mockMacGenerator.generateMac(
          any[String],
          any[String],
          any[String],
          any[Seq[String]],
          any[Option[String]],
          any[Option[String]],
          any[String],
          any[String]
        )
      ).thenReturn("valid-mac")

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService),
          bind[MacGenerator].toInstance(mockMacGenerator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to confirmation page if DirectDebitSource is 'CT' for a POST if all required data is provided" - {

      "and ddi reference is generated successfully" in {
        val paymentAmount = 200
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.CT)
          .setOrException(
            YourBankDetailsPage,
            YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false)
          )
          .setOrException(PaymentAmountPage, paymentAmount)
          .setOrException(
            PaymentDatePage,
            PaymentDateDetails(LocalDate.of(2025, 9, 15), "2025-09-01")
          )
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(pages.MacValuePage, "valid-mac")
          .setOrException(
            BankDetailsAddressPage,
            BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH"))
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .setOrException(pages.MacValuePage, "valid-mac")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(
          mockMacGenerator.generateMac(
            any[String],
            any[String],
            any[String],
            any[Seq[String]],
            any[Option[String]],
            any[Option[String]],
            any[String],
            any[String]
          )
        ).thenReturn("valid-mac")

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[MacGenerator].toInstance(mockMacGenerator)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DirectDebitConfirmationController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if CHRIS submission fails" in {
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, 5000)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .setOrException(pages.MacValuePage, "valid-mac")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.failed(new Exception("chris failed")))

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if CHRIS submission returns false" in {
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(
            YourBankDetailsPage,
            YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false)
          )
          .setOrException(TotalAmountDuePage, 5000)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(
            BankDetailsAddressPage,
            BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH"))
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .setOrException(pages.MacValuePage, "valid-mac")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.successful(false))

        when(
          mockMacGenerator.generateMac(
            any[String],
            any[String],
            any[String],
            any[Seq[String]],
            any[Option[String]],
            any[Option[String]],
            any[String],
            any[String]
          )
        ).thenReturn("valid-mac")

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[MacGenerator].toInstance(mockMacGenerator)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the generated MAC does not match the stored MAC" in {
        val userAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.CT)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(pages.MacValuePage, "stored-mac")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(
          mockMacGenerator.generateMac(
            any[String],
            any[String],
            any[String],
            any[Seq[String]],
            any[Option[String]],
            any[Option[String]],
            any[String],
            any[String]
          )
        ).thenReturn("generated-mac")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[MacGenerator].toInstance(mockMacGenerator)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if the generated MAC1 is not generated" in {
        val userAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.CT)
          .setOrException(PaymentReferencePage, "testReference")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(
          mockMacGenerator.generateMac(
            any[String],
            any[String],
            any[String],
            any[Seq[String]],
            any[Option[String]],
            any[Option[String]],
            any[String],
            any[String]
          )
        ).thenReturn("generated-mac")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[MacGenerator].toInstance(mockMacGenerator)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to confirmation page if DirectDebitSource is 'TC' and if all required data is provided" in {
        val totalDueAmount = 200
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, totalDueAmount)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .setOrException(pages.MacValuePage, "valid-mac")

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))
        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.successful(true))
        when(
          mockMacGenerator.generateMac(
            any[String],
            any[String],
            any[String],
            any[Seq[String]],
            any[Option[String]],
            any[Option[String]],
            any[String],
            any[String]
          )
        ).thenReturn("valid-mac")

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[MacGenerator].toInstance(mockMacGenerator),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DirectDebitConfirmationController.onPageLoad().url
        }
      }

    }

    "when set up a new payment plan for existing Direct Debit" - {

      "must redirect to confirmation page when DirectDebitSource is 'CT' for a POST if all required data is provided" in {

        val totalDueAmount = 200
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, totalDueAmount)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .set(
            ExistingDirectDebitIdentifierQuery,
            NddDetails("directDebitReference",
                       LocalDateTime.now(),
                       "bankSortCode",
                       "bankAccountNumber",
                       "bankAccountName",
                       auDdisFlag       = true,
                       numberOfPayPlans = 2
                      )
          )
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(false)))
        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DirectDebitConfirmationController.onPageLoad().url
        }
      }

      "must redirect to duplicate warning page 'CT' for a POST if all required data is provided" in {

        val totalDueAmount = 200
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, totalDueAmount)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .set(
            ExistingDirectDebitIdentifierQuery,
            NddDetails("directDebitReference",
                       LocalDateTime.now(),
                       "bankSortCode",
                       "bankAccountNumber",
                       "bankAccountName",
                       auDdisFlag       = true,
                       numberOfPayPlans = 2
                      )
          )
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DuplicateWarningForAddOrCreatePPController.onPageLoad(NormalMode).url
        }
      }

      "must redirect to duplicate error page 'MGD' for a POST if all required data is provided" in {

        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.MGD)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
          .setOrException(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage, BankAddress(Seq("line 1"), Some("Town"), Country("UK"), Some("NE5 2DH")))
          .setOrException(BankDetailsBankNamePage, "Barclays")
          .set(
            ExistingDirectDebitIdentifierQuery,
            NddDetails("directDebitReference",
                       LocalDateTime.now(),
                       "bankSortCode",
                       "bankAccountNumber",
                       "bankAccountName",
                       auDdisFlag       = true,
                       numberOfPayPlans = 2
                      )
          )
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        when(mockNddService.isDuplicatePlan(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(DuplicateCheckResponse(true)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DuplicateErrorController.onPageLoad().url
        }
      }
    }
  }
}
