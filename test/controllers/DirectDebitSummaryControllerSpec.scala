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
import models.{NddDetails, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.*
import repositories.{DirectDebitCacheRepository, SessionRepository}
import services.NationalDirectDebitService
import utils.DirectDebitDetailsData

import java.time.LocalDateTime
import scala.concurrent.Future

class DirectDebitSummaryControllerSpec extends SpecBase with DirectDebitDetailsData {

  "DirectDebitSummary Controller" - {

    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]
    val mockDirectDebitCache = mock[DirectDebitCacheRepository]

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
        html must include("Summary of payment plans for this Direct Debit")
      }
    }

    "must return OK and the correct view for a GET with a valid direct debit reference and empty payment plans" in {
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
          .thenReturn(Future.successful(mockDDPaymentPlansResponse.copy(paymentPlanCount = 0, paymentPlanList = Seq.empty)))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK

        val html = contentAsString(result)
        html must include("Set up a payment plan for this Direct Debit")
      }
    }

    "must redirect to System Error page when DirectDebitReferenceQuery is not set" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides()
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error page when UserAnswers is None" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides()
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SystemErrorController.onPageLoad().url
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

    "must redirect to direct debit source page when a directDebitReference is provided" in {
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
          bind[DirectDebitCacheRepository].toInstance(mockDirectDebitCache)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockDirectDebitCache.getDirectDebit(any())(any())).thenReturn(
          Future.successful(
            NddDetails("ddref", LocalDateTime.now(), "bankSortCode", "bankAccountNumber", "bankAccountName", auDdisFlag = true, numberOfPayPlans = 1)
          )
        )

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onRedirectToDirectDebitSource(directDebitReference).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DirectDebitSourceController.onPageLoad(NormalMode).url
      }
    }
  }
}
