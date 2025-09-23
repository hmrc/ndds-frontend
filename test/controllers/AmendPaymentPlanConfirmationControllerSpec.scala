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
import org.mockito.ArgumentMatchers.any
import models.{NormalMode, UserAnswers}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{AmendPaymentPlanSourcePage, AmendPaymentPlanTypePage, AmendPlanEndDatePage, AmendPlanStartDatePage, PaymentAmountPage, PaymentReferencePage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.DirectDebitReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DirectDebitDetailsData
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class AmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData{

  "PaymentPlanDetails Controller" - {

    val mockService = mock[NationalDirectDebitService]
    val mockSessionRepository = mock[SessionRepository]

    def createSummaryListForBudgetPaymentPlan(userAnswers: UserAnswers, app: Application): Seq[SummaryListRow] = {
      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers)(messages(app)),
        AmendPaymentPlanSourceSummary.row(userAnswers)(messages(app)),
        AmendPaymentReferenceSummary.row(userAnswers)(messages(app)),
        RegularPaymentAmountSummary.row(userAnswers)(messages(app)),
        PaymentAmountSummary.row(userAnswers)(messages(app)),
        AmendPlanEndDateSummary.row(userAnswers)(messages(app))
      ).flatten
    }

    def createSummaryListForOtherPaymentPlans(userAnswers: UserAnswers, app: Application): Seq[SummaryListRow] = {
      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers)(messages(app)),
        AmendPaymentPlanSourceSummary.row(userAnswers)(messages(app)),
        AmendPaymentReferenceSummary.row(userAnswers)(messages(app)),
        RegularPaymentAmountSummary.row(userAnswers)(messages(app)),
        PaymentAmountSummary.row(userAnswers)(messages(app)),
        AmendPlanStartDateSummary.row(userAnswers)(messages(app))
      ).flatten
    }

    "must return OK and the correct view for a GET with a Budget Payment Plan" in {
      val directDebitReference = "122222"
      val userAnswers =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value
          .set(
            AmendPaymentPlanTypePage,
            "budgetPaymentPlan"
          )
          .success
          .value
          .set(
            AmendPaymentPlanSourcePage,
            "paymentPlaneSource"
          )
          .success
          .value
          .set(
            PaymentReferencePage,
            "paymentPlaneSource"
          )
          .success
          .value
          .set(
            PaymentReferencePage,
            "paymentReference"
          )
          .success
          .value
          .set(
            PaymentAmountPage,
            150.0
          )
          .success
          .value
          .set(
            AmendPlanEndDatePage,
            LocalDate.now().plusDays(4)
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {

        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswers)))
        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(nddResponse))

        val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, application)
        val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
        status(result) mustEqual OK

        val directDebitDetails = nddResponse.directDebitList.head.toDirectDebitDetails

        contentAsString(result) mustEqual view(NormalMode, directDebitReference, directDebitDetails, summaryListRows)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with a Single Payment Plan" in {
      val directDebitReference = "122222"
      val userAnswers =
        emptyUserAnswers
          .set(
            DirectDebitReferenceQuery,
            directDebitReference
          )
          .success
          .value
          .set(
            AmendPaymentPlanTypePage,
            "SinglePaymentPlan"
          )
          .success
          .value
          .set(
            AmendPaymentPlanSourcePage,
            "paymentPlaneSource"
          )
          .success
          .value
          .set(
            PaymentReferencePage,
            "paymentPlaneSource"
          )
          .success
          .value
          .set(
            PaymentReferencePage,
            "paymentReference"
          )
          .success
          .value
          .set(
            PaymentAmountPage,
            150.0
          )
          .success
          .value
          .set(
            AmendPlanStartDatePage,
            LocalDate.now().plusDays(4)
          )
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[NationalDirectDebitService].toInstance(mockService)
        )
        .build()

      running(application) {

        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswers)))
        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(nddResponse))

        val summaryListRows = createSummaryListForOtherPaymentPlans(userAnswers, application)
        val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
        status(result) mustEqual OK

        val directDebitDetails = nddResponse.directDebitList.head.toDirectDebitDetails

        contentAsString(result) mustEqual view(NormalMode, directDebitReference, directDebitDetails, summaryListRows)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recover page when DirectDebitReferenceQuery is not set" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides().build()

      running(application) {

        val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recover page when DirectDebitReferenceQuery is not correct" in {
      val directDebitReference = "invalid ref number"
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

        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(userAnswersWithDirectDebitReference)))
        when(mockService.retrieveAllDirectDebits(any())(any(), any()))
          .thenReturn(Future.successful(nddResponse))

        val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
