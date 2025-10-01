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

import scala.math.BigDecimal.RoundingMode

object MonthlyPaymentAmountSummary {
  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TotalAmountDuePage).map { totalAmount =>
      val monthlyPayment = (totalAmount / 12).setScale(2, RoundingMode.DOWN)

      SummaryListRowViewModel(
        key = "totalAmountDue.monthly.checkYourAnswersLabel",
        value = ValueViewModel(currencyFormat(monthlyPayment)),
        actions = Seq.empty
      )
    }

  def row(amount: BigDecimal, totalDue: Option[BigDecimal])(implicit messages: Messages): SummaryListRow =
    totalDue.filter(_ > 0) match {
      case Some(_) =>
        SummaryListRowViewModel(
          key = "totalAmountDue.monthly.checkYourAnswersLabel",
          value = ValueViewModel(formatAmount(amount.doubleValue)),
          actions = Seq.empty
        )
      case None =>
        SummaryListRowViewModel(
          key = "totalAmountDue.monthly.checkYourAnswersLabel",
          value = ValueViewModel(""),
          actions = Seq.empty
        )
    }
}
