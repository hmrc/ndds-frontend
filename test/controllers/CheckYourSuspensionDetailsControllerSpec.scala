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
import models.{NormalMode, PaymentPlanType, SuspensionPeriodRange, UserAnswers}
import models.responses.{AmendLockResponse, PaymentPlanResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.{ManagePaymentPlanTypePage, SuspensionDetailsCheckYourAnswerPage, SuspensionPeriodRangeDatePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourSuspensionDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val mockSessionRepository = mock[SessionRepository]
  private val mockNddService = mock[NationalDirectDebitService]

  private val suspensionRange = SuspensionPeriodRange(
    startDate = LocalDate.of(2025, 10, 10),
    endDate   = LocalDate.of(2025, 12, 10)
  )

  private val userAnswersWithSuspensionRange: UserAnswers =
    emptyUserAnswers
      .set(SuspensionPeriodRangeDatePage, suspensionRange)
      .success
      .value
      .set(PaymentPlanDetailsQuery, dummyPlanDetailResponse)
      .success
      .value
      .set(PaymentPlanReferenceQuery, "PREF123")
      .success
      .value
      .set(DirectDebitReferenceQuery, "DDI123")
      .success
      .value
      .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
      .success
      .value

  "CheckYourSuspensionDetailsController" - {

    "must return OK and render the correct view for a GET in NormalMode" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithSuspensionRange))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        when(mockNddService.suspendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your suspension details")
        contentAsString(result) must include("10 Oct 2025 to 10 Dec 2025")
      }
    }

    "must redirect to not found page when user click back button from confirmation page" in {

      val userAnswersWithSuspensionRangeConfirm: UserAnswers =
        userAnswersWithSuspensionRange
          .set(SuspensionDetailsCheckYourAnswerPage, true)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSuspensionRangeConfirm))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        when(mockNddService.suspendPaymentPlanGuard(any())).thenReturn(true)
        val request = FakeRequest(GET, routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.BackSubmissionController.onPageLoad().url
      }
    }

    "must call CHRIS, update session, and redirect to Landing page when POST succeeds" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockNddService.submitChrisData(any())(any())) thenReturn Future.successful(true)
      when(mockNddService.lockPaymentPlan(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(AmendLockResponse(lockSuccessful = true)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSuspensionRange))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.PaymentPlanSuspendedController.onPageLoad(NormalMode).url

        verify(mockNddService, times(1)).submitChrisData(any())(any())
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to JourneyRecovery when CHRIS submission fails" in {
      when(mockNddService.submitChrisData(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSuspensionRange))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when DirectDebitReference is missing" in {
      val userAnswersWithoutDDI = userAnswersWithSuspensionRange.remove(DirectDebitReferenceQuery).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithoutDDI))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when PaymentPlanReference is missing" in {
      val userAnswersWithoutDDI = userAnswersWithSuspensionRange.remove(PaymentPlanReferenceQuery).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithoutDDI))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockNddService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery for a GET if no user answers are found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery for a POST if no user answers are found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
