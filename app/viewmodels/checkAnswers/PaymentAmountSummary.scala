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

package viewmodels.checkAnswers

import config.CurrencyFormatter.currencyFormat
import controllers.routes
import models.{CheckMode, PaymentPlanType, UserAnswers}
import pages.PaymentAmountPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PaymentAmountSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PaymentAmountPage).map { amount =>
      SummaryListRowViewModel(
        key   = "paymentAmount.checkYourAnswersLabel",
        value = ValueViewModel(currencyFormat(amount)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.PaymentAmountController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("paymentAmount.change.hidden"))
        )
      )
    }

  def row(planType: String, answers: UserAnswers, showChange: Boolean = false)(implicit messages: Messages): Option[SummaryListRow] =
    val label = if (PaymentPlanType.BudgetPaymentPlan.toString == planType) {
      "directDebitConfirmation.details.amount.budgetPaymentPlan"
    } else {
      "directDebitConfirmation.details.amount.singlePaymentPlan"
    }
    answers.get(PaymentAmountPage).map { amount =>
      SummaryListRowViewModel(
        key   = label,
        value = ValueViewModel(currencyFormat(amount)),
        actions = if (showChange) {
          Seq(
            ActionItemViewModel("site.change", routes.PaymentAmountController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("paymentAmount.change.hidden"))
          )
        } else {
          Seq.empty
        }
      )
    }
}
