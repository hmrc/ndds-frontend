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

import controllers.routes
import models.{CheckMode, PaymentPlanType, UserAnswers}
import pages.AmendPaymentAmountPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.MaskAndFormatUtils.*
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AmendPaymentAmountSummary {

  def row(planType: String, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    val label = if (PaymentPlanType.BudgetPaymentPlan.toString == planType) {
      "paymentPlanDetails.details.amount.budgetPaymentPlan"
    } else if (PaymentPlanType.TaxCreditRepaymentPlan.toString == planType) {
      "paymentPlanDetails.details.amount.taxCreditRepaymentPlan"
    } else {
      "paymentPlanDetails.details.amount.singlePaymentPlan"
    }

    answers.get(AmendPaymentAmountPage).map { amount =>
      SummaryListRowViewModel(
        key   = label,
        value = ValueViewModel(formatAmount(amount)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.AmendPaymentAmountController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amendPaymentAmount.change.hidden"))
        )
      )
    }
  }

  def row(planType: String, amount: Option[BigDecimal], showChange: Boolean = false)(implicit messages: Messages): SummaryListRow = {
    val label = if (PaymentPlanType.BudgetPaymentPlan.toString == planType) {
      "paymentPlanDetails.details.amount.budgetPaymentPlan"
    } else {
      "paymentPlanDetails.details.amount.singlePaymentPlan"
    }
    val displayValue = amount.map(a => formatAmount(a)).getOrElse("")
    SummaryListRowViewModel(
      key   = label,
      value = ValueViewModel(displayValue),
      actions = if (showChange) {
        Seq(
          ActionItemViewModel("site.change", controllers.testonly.routes.TestOnlyAmendPaymentAmountController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amendPaymentAmount.change.hidden"))
        )
      } else {
        Seq.empty
      }
    )
  }
}
