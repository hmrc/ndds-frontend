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
import models.responses.{BankAddress, Country, GenerateDdiRefResponse}
import models.{DirectDebitSource, PaymentDateDetails, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, YearEndAndMonth, YourBankDetailsWithAuddisStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.inject
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{AuditService, NationalDirectDebitService}
import viewmodels.govuk.SummaryListFluency

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val fixedDate = LocalDate.of(2025, 7, 19)
  private val paymentDateDetails: PaymentDateDetails = PaymentDateDetails(fixedDate, "2025-7-19")
  private val planStartDateDetails: PlanStartDateDetails = PlanStartDateDetails(fixedDate, "2025-7-19")
  private val endDate = LocalDate.of(2027, 7, 25)
  private val yearEndAndMonthDate = YearEndAndMonth(2025, 4)
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockNddService: NationalDirectDebitService = mock[NationalDirectDebitService]

  "Check Your Answers Controller" - {
    val userAnswer = emptyUserAnswers
      .setOrException(DirectDebitSourcePage, DirectDebitSource.CT)
      .setOrException(PaymentReferencePage, "1234567")
      .setOrException(PaymentAmountPage, 123.01)
      .setOrException(PaymentDatePage, paymentDateDetails)

    "must return OK and the correct view if CT selected for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("Payment Plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include("This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 July 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Accept and Continue")
      }
    }

    "must return OK and the correct view if MGD selected and type is single for a GET" in {
      val userAnswer = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.MGD)
        .setOrException(PaymentPlanTypePage, PaymentPlanType.SinglePayment)
        .setOrException(PaymentReferencePage, "1234567")
        .setOrException(PaymentAmountPage, 123.01)
        .setOrException(PaymentDatePage, paymentDateDetails)
      val application = applicationBuilder(userAnswers = Some(userAnswer)).build()
      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("Payment Plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include("This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 July 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Accept and Continue")
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
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("Payment Plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include("This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Payment amount")
        contentAsString(result) must include("123.01")
        contentAsString(result) must include("Payment date")
        contentAsString(result) must include("19 July 2025")
        contentAsString(result) must include("£123.01")
        contentAsString(result) must include("Year end and month")
        contentAsString(result) must include("2025 04")
        contentAsString(result) must include("Accept and Continue")
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
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("Payment Plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include("This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("Frequency of payments")
        contentAsString(result) must include("Monthly")
        contentAsString(result) must include("Regular Payment Amount")
        contentAsString(result) must include("19 July 2025")
        contentAsString(result) must include("25 July 2027")
        contentAsString(result) must include("£120")
        contentAsString(result) must include("Accept and Continue")
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
        contentAsString(result) must include("Check your answers")
        contentAsString(result) must include("Payment Plan details")
        contentAsString(result) must include("The Direct Debit Guarantee")
        contentAsString(result) must include("This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")
        contentAsString(result) must include("Payment reference")
        contentAsString(result) must include("1234567")
        contentAsString(result) must include("Total Amount Due")
        contentAsString(result) must include("4,533")
        contentAsString(result) must include("Plan Start Date")
        contentAsString(result) must include("19 July 2025")
        contentAsString(result) must include("Monthly Payment Amount")
        contentAsString(result) must include("£377.75")
        contentAsString(result) must include("Final Payment Date")
        contentAsString(result) must include("19 June 2026")
        contentAsString(result) must include("Final Payment Amount")
        contentAsString(result) must include("£377.75")
        contentAsString(result) must include("Accept and Continue")
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

    "must redirect to Journey Recovery for a POST if PaymentReferencePage data is missing" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
        .setOrException(YourBankDetailsPage,
          YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false)
        )
        .setOrException(BankDetailsAddressPage,
          BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
        )
        .setOrException(BankDetailsBankNamePage, "Barclays")
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)

      when(mockNddService.generateNewDdiReference(any())(any()))
        .thenReturn(Future.successful(GenerateDdiRefResponse("fakeRef")))

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = intercept[Exception](route(application, request).value.futureValue)

        result.getMessage must include("Missing details: paymentReference") // matches your controller's message
      }
    }

    "must redirect to Journey Recovery for a POST if DirectDebitSource data is missing" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(YourBankDetailsPage,
          YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false)
        )
        .setOrException(BankDetailsAddressPage,
          BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
        )
        .setOrException(BankDetailsBankNamePage, "Barclays")
        .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
        .setOrException(PaymentReferencePage, "testRef")

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(bind[NationalDirectDebitService].toInstance(mockNddService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)

        // route(...) returns Option[Future[Result]], safely extract Future
        val resultFuture: Future[play.api.mvc.Result] = route(application, request).get

        // Await the Future inside intercept to catch exception
        val exception = intercept[Exception] {
          await(resultFuture)
        }

        exception.getMessage must include("Missing details: directDebitSource")
      }
    }


    "must redirect to Journey Recovery for a POST if Ddi reference number is not obtained successfully" in {
      val incompleteAnswers = emptyUserAnswers
        .setOrException(PaymentReferencePage, "testReference")

      when(mockNddService.generateNewDdiReference(any())(any())).thenReturn(Future.failed(new Exception("bang")))
      // Mock CHRIS submission
      when(mockNddService.submitChrisData(any())(any()))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = intercept[Exception](route(application, request).value.futureValue)

        result.getMessage must include("bang")
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
          .setOrException(
            BankDetailsAddressPage,
            BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")

        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[AuditService].toInstance(mockAuditService) // ✅ override here
          )
          .build()

        doNothing().when(mockAuditService).sendSubmitDirectDebitPaymentPlan(any(), any())

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
          .setOrException(YourBankDetailsPage,
            YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, 5000)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage,
            BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")

        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.failed(new Exception("chris failed")))

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService),
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
            BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")

        when(mockNddService.generateNewDdiReference(any())(any()))
          .thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))

        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.successful(false))

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          flash(result).get("error") mustBe Some("There was a problem submitting your direct debit. Please try again later.")
        }
      }

      "must redirect to confirmation page if DirectDebitSource is 'TC' and send an audit event for a POST if all required data is provided" in {
        val totalDueAmount = 200
        val incompleteAnswers = emptyUserAnswers
          .setOrException(DirectDebitSourcePage, DirectDebitSource.TC)
          .setOrException(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .setOrException(YourBankDetailsPage,
            YourBankDetailsWithAuddisStatus("Test", "123456", "12345678", false, false))
          .setOrException(TotalAmountDuePage, totalDueAmount)
          .setOrException(PlanStartDatePage, planStartDateDetails)
          .setOrException(PaymentReferencePage, "testReference")
          .setOrException(BankDetailsAddressPage,
            BankAddress(Seq("line 1"), "Town", Country("UK"), "NE5 2DH")
          )
          .setOrException(BankDetailsBankNamePage, "Barclays")

        when(mockNddService.generateNewDdiReference(any())(any())).thenReturn(Future.successful(GenerateDdiRefResponse("testRefNo")))
        when(mockNddService.submitChrisData(any())(any()))
          .thenReturn(Future.successful(true))
        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService),
            bind[NationalDirectDebitService].toInstance(mockNddService)
          )
          .build()

        doNothing().when(mockAuditService).sendSubmitDirectDebitPaymentPlan(any(), any())

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DirectDebitConfirmationController.onPageLoad().url
        }
      }
    }

  }
}
