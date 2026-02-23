/*
 * Copyright 2026 HM Revenue & Customs
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
import models.NddResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.NationalDirectDebitService
import splitter.connectors.AllowListConnector
import splitter.controllers.{FakeIdentityIdentifierAction, IdentityIdentifierAction}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class LandingControllerSpec extends SpecBase {

  class FakeAllowListConnector(result: Exception | Boolean) extends AllowListConnector:
    override def check(userId: String)(using HeaderCarrier): Future[Boolean] =
      result match
        case res: Boolean   => Future.successful(res)
        case res: Exception => Future.failed(res)

  private def legacyUrl = "/national-direct-debits"

  "Landing Controller" - {
    val mockService = mock[NationalDirectDebitService]

    "must return REDIRECT and the correct view for a GET with no existing debits" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService),
          bind[IdentityIdentifierAction].to[FakeIdentityIdentifierAction],
          bind[AllowListConnector].toInstance(FakeAllowListConnector(true))
        )
        .build()

      running(application) {
        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(NddResponse(0, Seq())))

        val request = FakeRequest(GET, routes.LandingController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SetupDirectDebitPaymentController.onPageLoad().url
      }
    }

    "must return REDIRECT and the correct view for a GET with existing debits" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService),
          bind[IdentityIdentifierAction].to[FakeIdentityIdentifierAction],
          bind[AllowListConnector].toInstance(FakeAllowListConnector(true))
        )
        .build()

      running(application) {

        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(NddResponse(2, Seq())))

        val request = FakeRequest(GET, routes.LandingController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.YourDirectDebitInstructionsController.onPageLoad().url
      }
    }

    "must return REDIRECT and the correct view for an unauthenticated user" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService),
          bind[IdentityIdentifierAction].to[FakeIdentityIdentifierAction],
          bind[AllowListConnector].toInstance(FakeAllowListConnector(true))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.LandingController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.YourDirectDebitInstructionsController.onPageLoad().url
      }
    }

    "must return REDIRECT to legacy url when check is false" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService),
          bind[IdentityIdentifierAction].to[FakeIdentityIdentifierAction],
          bind[AllowListConnector].toInstance(FakeAllowListConnector(false))
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.LandingController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe legacyUrl
      }
    }

  }
}
