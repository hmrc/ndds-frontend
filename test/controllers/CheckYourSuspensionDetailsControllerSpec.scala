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
import models.{NormalMode, SuspensionPeriodRange, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.SuspensionPeriodRangeDatePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class CheckYourSuspensionDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val mockSessionRepository = mock[SessionRepository]
  private val suspensionRange = SuspensionPeriodRange(
    startDate = java.time.LocalDate.of(2025, 10, 10),
    endDate   = java.time.LocalDate.of(2025, 12, 10)
  )

  private val userAnswers: UserAnswers = emptyUserAnswers
    .set(SuspensionPeriodRangeDatePage, suspensionRange)
    .success
    .value

  "CheckYourSuspensionDetailsController" - {

    "must return OK and the correct view for a GET in NormalMode" in {
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your suspension details ")
        contentAsString(result) must include("10 Oct 2025 to 10 Dec 2025")
      }
    }

    "must redirect to the next page on POST" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourSuspensionDetailsController.onSubmit(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.LandingController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no data is found" in {
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
