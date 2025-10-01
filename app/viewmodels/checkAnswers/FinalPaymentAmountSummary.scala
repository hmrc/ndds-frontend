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
import models.UserAnswers
import pages.TotalAmountDuePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object FinalPaymentAmountSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TotalAmountDuePage).map { totalAmount =>
      val monthlyPayment = (totalAmount / 12).setScale(2, BigDecimal.RoundingMode.DOWN)
      val finalPayment = totalAmount - (monthlyPayment * 11)
      SummaryListRowViewModel(
        key = "totalAmountDue.final.checkYourAnswersLabel",
        value = ValueViewModel(currencyFormat(finalPayment)),
        actions = Seq.empty
      )
    }

  def row(amount: Option[BigDecimal], totalDue: Option[BigDecimal])(implicit messages: Messages): SummaryListRow = {
    val displayValue =
      if (totalDue.exists(_ > 0) && amount.isDefined) {
        formatAmount(amount.get.doubleValue)
      } else {
        ""
      }

    SummaryListRowViewModel(
      key = "totalAmountDue.final.checkYourAnswersLabel",
      value = ValueViewModel(displayValue),
      actions = Seq.empty
    )
  }

}
