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
import models.{RDSDatacacheResponse, RDSDirectDebitDetails}
import models.responses.LockResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{LockService, RDSDatacacheService}
import views.html.SetupDirectDebitPaymentView
import play.api.mvc.Call

import java.time.Instant
import scala.concurrent.Future

class SetupDirectDebitPaymentControllerSpec extends SpecBase {

  "SetupDirectDebitPayment Controller" - {

    val mockService = mock[LockService]
    val returnedDate = "2025-06-28T15:30:30Z"

    def response = LockResponse(
      _id = "testId",
      verifyCalls = 3,
      isLocked = false,
      unverifiable = None,
      createdAt = None,
      lastUpdated = None,
      lockoutExpiryDateTime = Some(Instant.parse(returnedDate))
    )

    lazy val yourDirectDebitInstructionsRoute: String = routes.YourDirectDebitInstructionsController.onPageLoad().url

    val mockRDSDatacacheService: RDSDatacacheService = mock[RDSDatacacheService]
    val sequence: Seq[RDSDirectDebitDetails] = Seq.empty

    "must return OK and the correct view for a GET with no back link (DDI = 0) without Back link" in {
      val cacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(0, sequence)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[RDSDatacacheService].toInstance(mockRDSDatacacheService),
        bind[LockService].toInstance(mockService)
      ).build()

      running(application) {

        when(mockRDSDatacacheService.retrieveAllDirectDebits(any())(any(), any()))
                .thenReturn(Future.successful(cacheResponse))

        when(mockService.isUserLocked(any())(any()))
          .thenReturn(Future.successful(response))

        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SetupDirectDebitPaymentView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(0, Call(GET, yourDirectDebitInstructionsRoute))(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET if there is back link (DDI > 1) with Back link" in {
      val cacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(3, sequence)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[RDSDatacacheService].toInstance(mockRDSDatacacheService),
        bind[LockService].toInstance(mockService)
      ).build()

      running(application) {

        when(mockRDSDatacacheService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(cacheResponse))

        when(mockService.isUserLocked(any())(any()))
          .thenReturn(Future.successful(response))

        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("Back")
        contentAsString(result) must include("Setup a direct debit payment")
        contentAsString(result) must include("Please note")
        contentAsString(result) must include("Start now")
      }

    }

    "must return See Other and redirect for a GET if the user is locked but not verified" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[LockService].toInstance(mockService)
      ).build()

      running(application) {

        when(mockService.isUserLocked(any())(any()))
          .thenReturn(Future.successful(response.copy(isLocked = true, unverifiable = Some(true))))

        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual routes.ReachedLimitController.onPageLoad().url
      }
    }

    "must return See Other and redirect for a GET if the user is locked and verified" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(
        bind[LockService].toInstance(mockService)
      ).build()

      running(application) {

        when(mockService.isUserLocked(any())(any()))
          .thenReturn(Future.successful(response.copy(isLocked = true, unverifiable = Some(false))))

        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustEqual routes.AccountDetailsNotVerifiedController.onPageLoad().url
      }
    }
  }
}

