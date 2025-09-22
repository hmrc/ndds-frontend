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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import utils.DirectDebitDetailsData
import views.html.PaymentPlanDetailsView

import scala.concurrent.Future

class PaymentPlanDetailsControllerSpec extends SpecBase with DirectDebitDetailsData {

  "PaymentPlanDetails Controller" - {

    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    "must return OK and the correct view for a GET with a valid direct payment Reference" in {
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswersWithPaymentReference)))
        when(mockService.getPaymentPlanDetails(any()))
          .thenReturn(Future.successful(mockPaymentPlanDetailResponse))
        when(mockService.isThreeDaysPriorPlanEndDate(any())(any()))
          .thenReturn(Future.successful(true))
        when(mockService.isTwoDaysPriorPaymentDate(any())(any()))
          .thenReturn(Future.successful(false))

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentPlanDetailsView]
        status(result) mustEqual OK
        println(contentAsString(result))
        contentAsString(result) mustEqual view(paymentReference, mockPaymentPlanDetailResponse, true, false)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recover page when DirectDebitReferenceQuery is not set" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides().build()

      running(application) {

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Payment Plan Details page when a PaymentReferenceQuery is provided" in {
      val paymentReference = "paymentReference"
      val userAnswersWithPaymentReference =
        emptyUserAnswers
          .set(
            PaymentReferenceQuery,
            paymentReference
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPaymentReference))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.PaymentPlanDetailsController.onRedirect(paymentReference).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentPlanDetailsController.onPageLoad().url

        verify(mockSessionRepository).set(eqTo(userAnswersWithPaymentReference))
      }
    }
  }
}
