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
import models.responses.LockResponse
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.LockService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.AccountDetailsNotVerifiedView

import java.time.Instant
import scala.concurrent.Future

class AccountDetailsNotVerifiedControllerSpec extends SpecBase with MockitoSugar {

  "AccountDetailsNotVerified Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockLockService = mock[LockService]
      val testInstant = Instant.parse("2025-06-28T15:30:00Z")

      when(mockLockService.isUserLocked(any[String])(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            LockResponse(
              _id                   = "",
              verifyCalls           = 0,
              isLocked              = true,
              unverifiable          = None,
              createdAt             = None,
              lastUpdated           = None,
              lockoutExpiryDateTime = Some(testInstant)
            )
          )
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[LockService].toInstance(mockLockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AccountDetailsNotVerifiedController.onPageLoad().url)
        val result = route(application, request).value

        val view = application.injector.instanceOf[AccountDetailsNotVerifiedView]
        val expectedDate = "28 June 2025, 16:30pm"

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedDate)(request, messages(application)).toString
      }
    }
  }
}
