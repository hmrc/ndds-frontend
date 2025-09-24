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
import models.{NormalMode, UserAnswers, YourBankDetailsWithAuddisStatus}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{DirectDebitReferenceQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DirectDebitDetailsData
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import java.time.LocalDate
import scala.concurrent.Future

class AmendPaymentPlanConfirmationControllerSpec extends SpecBase with DirectDebitDetailsData {

  "PaymentPlanDetails Controller" - {

    val mockSessionRepository = mock[SessionRepository]

    def createSummaryListForBudgetPaymentPlan(userAnswers: UserAnswers, app: Application): Seq[SummaryListRow] = {
      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers)(messages(app)),
        AmendPaymentPlanSourceSummary.row(userAnswers)(messages(app)),
        AmendPaymentReferenceSummary.row(userAnswers)(messages(app)),
        AmendPaymentAmountSummary.row(userAnswers)(messages(app)),
        AmendPlanEndDateSummary.row(userAnswers)(messages(app))
      ).flatten
    }

    def createSummaryListForOtherPaymentPlans(userAnswers: UserAnswers, app: Application): Seq[SummaryListRow] = {
      Seq(
        AmendPaymentPlanTypeSummary.row(userAnswers)(messages(app)),
        AmendPaymentPlanSourceSummary.row(userAnswers)(messages(app)),
        AmendPaymentReferenceSummary.row(userAnswers)(messages(app)),
        AmendPaymentAmountSummary.row(userAnswers)(messages(app)),
        AmendPlanStartDateSummary.row(userAnswers)(messages(app))
      ).flatten
    }

    "onPageLoad" - {
      "must return OK and the correct view for a GET with a Budget Payment Plan" in {
        val directDebitReference = "122222"
        val paymentReference = "paymentReference"
        val userAnswers =
          emptyUserAnswers
            .set(
              YourBankDetailsPage,
              YourBankDetailsWithAuddisStatus(
                accountHolderName = "account name",
                sortCode = "sort code",
                accountNumber = "account number",
                auddisStatus = false,
                accountVerified = false
              )
            ).success.value
            .set(
              DirectDebitReferenceQuery,
              directDebitReference
            ).success.value
            .set(
              PaymentReferenceQuery,
              "payment reference"
            ).success.value
            .set(
              AmendPaymentPlanTypePage,
              "budgetPaymentPlan"
            ).success.value
            .set(
              AmendPaymentPlanSourcePage,
              "paymentPlaneSource"
            ).success.value
            .set(
              PaymentReferencePage,
              paymentReference
            ).success.value
            .set(
              AmendPaymentAmountPage,
              150.0
            ).success.value
            .set(
              AmendPlanStartDatePage,
              LocalDate.now().plusDays(4)
            ).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForBudgetPaymentPlan(userAnswers, application)
          val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(NormalMode, paymentReference, directDebitReference, "sort code",
            "account number", summaryListRows, routes.AmendPlanEndDateController.onPageLoad(NormalMode))(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET with a Single Payment Plan" in {
        val directDebitReference = "122222"
        val paymentReference = "paymentReference"
        val userAnswers =
          emptyUserAnswers
            .set(
              YourBankDetailsPage,
              YourBankDetailsWithAuddisStatus(
                accountHolderName = "account name",
                sortCode = "sort code",
                accountNumber = "account number",
                auddisStatus = false,
                accountVerified = false
              )
            ).success.value
            .set(
              DirectDebitReferenceQuery,
              directDebitReference
            ).success.value
            .set(
              PaymentReferenceQuery,
              "payment reference"
            ).success.value
            .set(
              AmendPaymentPlanTypePage,
              "singlePaymentPlan"
            ).success.value
            .set(
              AmendPaymentPlanSourcePage,
              "paymentPlanSource"
            ).success.value
            .set(
              PaymentReferencePage,
              paymentReference
            ).success.value
            .set(
              AmendPaymentAmountPage,
              150.0
            ).success.value
            .set(
              AmendPlanEndDatePage,
              LocalDate.now().plusDays(4)
            ).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {

          when(mockSessionRepository.get(any()))
            .thenReturn(Future.successful(Some(userAnswers)))

          val summaryListRows = createSummaryListForOtherPaymentPlans(userAnswers, application)
          val request = FakeRequest(GET, routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AmendPaymentPlanConfirmationView]
          status(result) mustEqual OK

          contentAsString(result) mustEqual view(NormalMode, paymentReference, directDebitReference, "sort code",
            "account number", summaryListRows, routes.AmendPlanStartDateController.onPageLoad(NormalMode))(request, messages(application)).toString
        }
      }
    }
  }
}
