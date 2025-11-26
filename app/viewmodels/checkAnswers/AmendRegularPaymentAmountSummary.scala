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
import models.{CheckMode, UserAnswers}
import pages.RegularPaymentAmountPage
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AmendRegularPaymentAmountSummary {

  def row(
    answers: UserAnswers,
    changeCall: Option[Call] = Some(routes.RegularPaymentAmountController.onPageLoad(CheckMode))
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegularPaymentAmountPage).map { answer =>
      buildRow(Some(answer), showChange = true, changeCall)
    }

  def row(
    amount: Option[BigDecimal],
    showChange: Boolean,
    changeCall: Option[Call]
  )(implicit messages: Messages): SummaryListRow =
    buildRow(amount, showChange, changeCall)

  private def buildRow(
    amount: Option[BigDecimal],
    showChange: Boolean,
    changeCall: Option[Call]
  )(implicit messages: Messages): SummaryListRow = {
    val actions =
      if (showChange) {
        changeCall
          .map(call =>
            Seq(
              ActionItemViewModel("site.change", call.url)
                .withVisuallyHiddenText(messages("regularPaymentAmount.change.hidden"))
            )
          )
          .getOrElse(Seq.empty)
      } else {
        Seq.empty
      }

    SummaryListRowViewModel(
      key     = "regularPaymentAmount.checkYourAnswersLabel",
      value   = ValueViewModel(amount.map(currencyFormat).getOrElse("")),
      actions = actions
    )
  }
}
