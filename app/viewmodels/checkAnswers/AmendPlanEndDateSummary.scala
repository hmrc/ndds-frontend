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
import models.{CheckMode, NormalMode, UserAnswers}
import pages.AmendPlanEndDatePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AmendPlanEndDateSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AmendPlanEndDatePage).map { answer =>
      SummaryListRowViewModel(
        key   = "amendPlanEndDate.endDate",
        value = ValueViewModel(answer.format(DateTimeFormatter.ofPattern("d MMM yyyy"))),
        actions = Seq(
          ActionItemViewModel("site.change", routes.AmendPlanEndDateController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amendPlanEndDate.endDate"))
        )
      )
    }

  def row(value: Option[LocalDate], dateFormatter: String, showChange: Boolean = false)(implicit messages: Messages): SummaryListRow = {
    val formatter = DateTimeFormatter.ofPattern(dateFormatter, messages.lang.locale)
    val displayValue = value.map(_.format(formatter)).getOrElse("")
    SummaryListRowViewModel(
      key   = "amendPlanEndDate.endDate",
      value = ValueViewModel(displayValue),
      actions = if (showChange) {
        Seq(
          ActionItemViewModel("site.change", routes.AmendPlanEndDateController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amendPlanEndDate.change.endDate.hidden")),
          ActionItemViewModel("site.remove", routes.AmendConfirmRemovePlanEndDateController.onPageLoad(NormalMode).url)
            .withVisuallyHiddenText(messages("amendPlanEndDate.remove.endDate.hidden"))
        )
      } else {
        Seq.empty
      }
    )
  }

  def addRow()(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(messages("amendPlanEndDate.endDate"))),
      value = Value(
        HtmlContent(
          s"""<a class="govuk-link" href="${routes.AmendPlanEndDateController.onPageLoad(NormalMode).url}">${messages(
              "amendPlanEndDate.addPlanEndDateLink"
            )}</a>"""
        )
      ),
      actions = None
    )

  def rowData(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    val dateText = answers
      .get(AmendPlanEndDatePage)
      .map(_.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
      .getOrElse("")

    Some(
      SummaryListRowViewModel(
        key = "amendPlanEndDate.endDate",
        ValueViewModel(dateText),
        actions = Seq.empty
      )
    )
  }
}
