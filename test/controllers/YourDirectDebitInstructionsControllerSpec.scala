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
import models.{NddDetails, NddResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import pages.CreateConfirmationPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, ExistingDirectDebitIdentifierQuery, PaymentPlansCountQuery}
import repositories.SessionRepository
import services.{NationalDirectDebitService, PaginationResult, PaginationService}
import viewmodels.govuk.PaginationFluency.*

import java.time.LocalDateTime
import scala.concurrent.Future

class YourDirectDebitInstructionsControllerSpec extends SpecBase with Matchers with MockitoSugar {

  "YourDirectDebitInstructionsController" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET with no page parameter" in {
        val testData = createTestNddResponse(5)
        val mockNddService = mock[NationalDirectDebitService]
        val mockPaginationService = mock[PaginationService]
        val mockSessionRepository = mock[SessionRepository]

        when(mockNddService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(testData))
        when(mockPaginationService.paginateDirectDebits(any(), any(), any()))
          .thenReturn(createTestPaginationResult(1, 3, 2))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[PaginationService].toInstance(mockPaginationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockPaginationService).paginateDirectDebits(mockitoEq(testData.directDebitList), mockitoEq(1), any())
        }
      }

      "must return OK and the correct view for a GET with page parameter" in {
        val testData = createTestNddResponse(10)
        val mockNddService = mock[NationalDirectDebitService]
        val mockPaginationService = mock[PaginationService]
        val mockSessionRepository = mock[SessionRepository]

        when(mockNddService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(testData))
        when(mockPaginationService.paginateDirectDebits(any(), any(), any()))
          .thenReturn(createTestPaginationResult(2, 3, 4))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[PaginationService].toInstance(mockPaginationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url + "?page=2")
          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockPaginationService).paginateDirectDebits(mockitoEq(testData.directDebitList), mockitoEq(2), any())
        }
      }

      "must handle invalid page parameter gracefully" in {
        val testData = createTestNddResponse(5)
        val mockNddService = mock[NationalDirectDebitService]
        val mockPaginationService = mock[PaginationService]
        val mockSessionRepository = mock[SessionRepository]

        when(mockNddService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(testData))
        when(mockPaginationService.paginateDirectDebits(any(), any(), any()))
          .thenReturn(createTestPaginationResult(1, 3, 2))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[PaginationService].toInstance(mockPaginationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url + "?page=invalid")
          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockPaginationService).paginateDirectDebits(mockitoEq(testData.directDebitList), mockitoEq(1), any())
        }
      }

      "must handle empty direct debit list" in {
        val emptyData = NddResponse(0, Seq.empty)
        val mockNddService = mock[NationalDirectDebitService]
        val mockPaginationService = mock[PaginationService]
        val mockSessionRepository = mock[SessionRepository]

        when(mockNddService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(emptyData))
        when(mockPaginationService.paginateDirectDebits(any(), any(), any()))
          .thenReturn(createTestPaginationResult(1, 0, 0))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[PaginationService].toInstance(mockPaginationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockPaginationService).paginateDirectDebits(mockitoEq(Seq.empty), mockitoEq(1), any())
        }
      }

      "must clear ExistingDirectDebitIdentifierQuery from session" in {
        val testData = createTestNddResponse(1)
        val mockNddService = mock[NationalDirectDebitService]
        val mockPaginationService = mock[PaginationService]
        val mockSessionRepository = mock[SessionRepository]

        when(mockNddService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(testData))
        when(mockPaginationService.paginateDirectDebits(any(), any(), any()))
          .thenReturn(createTestPaginationResult(1, 1, 1))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val testNddDetails = NddDetails(
          ddiRefNumber       = "700000040",
          submissionDateTime = LocalDateTime.now(),
          bankSortCode       = "123456",
          bankAccountNumber  = "12345678",
          bankAccountName    = "Test Account",
          auDdisFlag         = false,
          numberOfPayPlans   = 2
        )

        val userAnswersWithExistingDDI = emptyUserAnswers
          .set(ExistingDirectDebitIdentifierQuery, testNddDetails)
          .success
          .value
          .set(DirectDebitReferenceQuery, "testDDRef")
          .success
          .value
          .set(PaymentPlansCountQuery, 2)
          .success
          .value
          .set(CreateConfirmationPage, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithExistingDDI))
          .overrides(
            bind[NationalDirectDebitService].toInstance(mockNddService),
            bind[PaginationService].toInstance(mockPaginationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourDirectDebitInstructionsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK

          val captor: ArgumentCaptor[models.UserAnswers] = ArgumentCaptor.forClass(classOf[models.UserAnswers])
          verify(mockSessionRepository, atLeastOnce()).set(captor.capture())

          val savedAnswers = captor.getValue
          savedAnswers.get(ExistingDirectDebitIdentifierQuery) mustBe None
          savedAnswers.get(DirectDebitReferenceQuery) mustBe None
          savedAnswers.get(PaymentPlansCountQuery) mustBe None
          savedAnswers.get(CreateConfirmationPage) mustBe None
        }
      }
    }
  }

  private def createTestNddResponse(count: Int): NddResponse = {
    val now = LocalDateTime.now()
    val testDetails = (1 to count).map { i =>
      NddDetails(
        ddiRefNumber       = s"DD$i",
        submissionDateTime = now.minusDays(i),
        bankSortCode       = "123456",
        bankAccountNumber  = "12345678",
        bankAccountName    = s"Test Account $i",
        auDdisFlag         = false,
        numberOfPayPlans   = 1
      )
    }
    NddResponse(count, testDetails)
  }

  private def createTestPaginationResult(currentPage: Int, totalRecords: Int, totalPages: Int): PaginationResult = {
    PaginationResult(
      paginatedData       = Seq.empty,
      paginationViewModel = PaginationViewModel(),
      totalRecords        = totalRecords,
      currentPage         = currentPage,
      totalPages          = totalPages
    )
  }
}
