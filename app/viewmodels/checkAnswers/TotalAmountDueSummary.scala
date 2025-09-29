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
import pages.TotalAmountDuePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object TotalAmountDueSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TotalAmountDuePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "totalAmountDue.checkYourAnswersLabel",
          value   = ValueViewModel(currencyFormat(answer)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TotalAmountDueController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("totalAmountDue.change.hidden"))
          )
        )
    }

  def row(amount: BigDecimal)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "totalAmountDue.checkYourAnswersLabel",
      value = ValueViewModel(formatAmount(amount.doubleValue)),
      actions = Seq.empty
    )

}
