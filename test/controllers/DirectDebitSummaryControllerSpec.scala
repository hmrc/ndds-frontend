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
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.*
import pages.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import utils.DirectDebitDetailsData

import scala.concurrent.Future

class DirectDebitSummaryControllerSpec extends SpecBase with DirectDebitDetailsData {

  "DirectDebitSummary Controller" - {

    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "must return OK and the correct view for a GET with a valid direct debit reference" in {
      val directDebitReference = "ref number 1"
      val userAnswersWithDirectDebitReference =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDirectDebitReference))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswersWithDirectDebitReference)))
        when(mockService.retrieveDirectDebitPaymentPlans(any(), any())(any(), any()))
          .thenReturn(Future.successful(mockDDPaymentPlansResponse))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val html = contentAsString(result)
        html must include(directDebitReference)
        html must include("Payment reference")
        html must include("Plan type")
        html must include("Payment amount")
        html must include("Manage plan")
      }
    }

    "must redirect to Journey Recovery page when DirectDebitReferenceQuery is not set" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides()
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery page when UserAnswers is None" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides()
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to summary payment plans page when a directDebitReference is provided" in {
      val directDebitReference = "ref number 1"
      val userAnswersWithDirectDebitReference =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDirectDebitReference))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onRedirect(directDebitReference).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DirectDebitSummaryController.onPageLoad().url

        verify(mockSessionRepository).set(eqTo(userAnswersWithDirectDebitReference))
      }
    }

    "must redirect to summary payment plans page when UserAnswers is None and directDebitReference is provided" in {
      val directDebitReference = "ref number 1"
      val userAnswersWithDirectDebitReference =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(userAnswersWithDirectDebitReference)).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onRedirect(directDebitReference).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DirectDebitSummaryController.onPageLoad().url
      }
    }

    "must call cleansePaymentReference and return OK with the correct view" in {
      val directDebitReference = "ref number 1"
      val userAnswersWithDirectDebitReference =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value
          .set(
            PaymentPlansCountQuery,
            2
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDirectDebitReference))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswersWithDirectDebitReference)))
        when(mockService.retrieveDirectDebitPaymentPlans(any(), any())(any(), any()))
          .thenReturn(Future.successful(mockDDPaymentPlansResponse))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK

        val captor = ArgumentCaptor.forClass(classOf[models.UserAnswers])
        verify(mockSessionRepository, times(6)).set(captor.capture())

        val capturedList = captor.getAllValues

        val firstCaptured = capturedList.get(0)

        firstCaptured.get(PaymentPlanReferenceQuery) mustBe None
        firstCaptured.get(PaymentPlanDetailsQuery) mustBe None
        firstCaptured.get(ManagePaymentPlanTypePage) mustBe None
        firstCaptured.get(AmendPaymentAmountPage) mustBe None
        firstCaptured.get(AmendPlanStartDatePage) mustBe None
        firstCaptured.get(AmendPlanEndDatePage) mustBe None
        firstCaptured.get(SuspensionPeriodRangeDatePage) mustBe None
      }
    }
  }
}
