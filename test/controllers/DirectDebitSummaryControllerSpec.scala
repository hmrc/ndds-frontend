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
import models.DirectDebitDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.NationalDirectDebitService
import utils.DirectDebitDetailsData
import views.html.DirectDebitSummaryView

import scala.concurrent.Future

class DirectDebitSummaryControllerSpec extends SpecBase with DirectDebitDetailsData {

  "DirectDebitSummary Controller" - {

    val mockService = mock[NationalDirectDebitService]

    "must return OK and the correct view for a GET with a valid direct debit reference" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {

        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(nddResponse))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad("122222").url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DirectDebitSummaryView]
        val directDebitDetails = DirectDebitDetails(
          directDebitReference ="122222",
          setupDate="1 February 2024",
          sortCode = "666666",
          accountNumber = "00000000",
          paymentPlans = "0"
        )
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(directDebitDetails,
          routes.YourDirectDebitInstructionsController.onPageLoad())(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {
        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(nddResponse))

        val request = FakeRequest(GET, routes.DirectDebitSummaryController.onPageLoad("invalid direct debit reference").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}

