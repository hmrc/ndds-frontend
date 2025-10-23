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
import models.responses.PaymentPlanDetails
import models.{PaymentPlanType, UserAnswers}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.*
import views.html.RemoveSuspensionConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RemoveSuspensionConfirmationControllerSpec extends SpecBase {

  private lazy val removeSuspensionConfirmationRoute = routes.RemoveSuspensionConfirmationController.onPageLoad().url

  val regPaymentAmount: BigDecimal = BigDecimal("1000.00")
  val formattedRegPaymentAmount: String = formatAmount(regPaymentAmount)
  val startDate: LocalDate = LocalDate.of(2025, 10, 2)
  val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  val endDate: LocalDate = LocalDate.of(2025, 10, 25)

  "RemoveSuspensionConfirmation Controller" - {

    "must return OK and the correct view for a GET with BudgetPaymentPlan (with and without Plan End Date)" in {

      def summaryList(userAnswers: UserAnswers, paymentPlanReference: String)(implicit messages: Messages): Seq[SummaryListRow] = {
        val baseRows = Seq(
          PaymentReferenceSummary.row(paymentPlanReference),
          AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, userAnswers.get(AmendPaymentAmountPage)),
          AmendPlanStartDateSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, userAnswers.get(AmendPlanStartDatePage), Constants.longDateTimeFormatPattern)
        )

        userAnswers.get(AmendPlanEndDatePage) match {
          case Some(endDate) => baseRows :+ AmendPlanEndDateSummary.row(Some(endDate), Constants.longDateTimeFormatPattern)
          case None => baseRows
        }
      }

      val baseAnswers = emptyUserAnswers
        .set(PaymentPlanReferenceQuery, "987654321L").success.value
        .set(AmendPaymentAmountPage, regPaymentAmount).success.value
        .set(AmendPlanStartDatePage, startDate).success.value
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString).success.value

      // With Plan End Date
      val userAnswersWithEndDate = baseAnswers
        .set(AmendPlanEndDatePage, endDate).success.value

      val applicationWithEndDate = applicationBuilder(userAnswers = Some(userAnswersWithEndDate)).build()

      running(applicationWithEndDate) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(applicationWithEndDate, request).value
        val view = applicationWithEndDate.injector.instanceOf[RemoveSuspensionConfirmationView]

        val formattedRegPaymentAmount = formatAmount(userAnswersWithEndDate.get(AmendPaymentAmountPage).get)
        val formattedStartDate = userAnswersWithEndDate.get(AmendPlanStartDatePage).get.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val summaryListRows = summaryList(userAnswersWithEndDate, "987654321L")(messages(applicationWithEndDate))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          formattedRegPaymentAmount,
          formattedStartDate,
          summaryListRows,
          routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(applicationWithEndDate)).toString
      }

      // Without Plan End Date
      val applicationWithoutEndDate = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(applicationWithoutEndDate) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(applicationWithoutEndDate, request).value
        val view = applicationWithoutEndDate.injector.instanceOf[RemoveSuspensionConfirmationView]

        val formattedRegPaymentAmount = formatAmount(baseAnswers.get(AmendPaymentAmountPage).get)
        val formattedStartDate = baseAnswers.get(AmendPlanStartDatePage).get.format(DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern))
        val summaryListRows = summaryList(baseAnswers, "987654321L")(messages(applicationWithoutEndDate))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          formattedRegPaymentAmount,
          formattedStartDate,
          summaryListRows,
          routes.PaymentPlanDetailsController.onPageLoad()
        )(request, messages(applicationWithoutEndDate)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if empty data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeSuspensionConfirmationRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
