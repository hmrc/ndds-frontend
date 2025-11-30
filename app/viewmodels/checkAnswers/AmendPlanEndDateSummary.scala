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
        key   = "amendPaymentPlanConfirmation.amendPaymentPlan.endDate",
        value = ValueViewModel(answer.format(DateTimeFormatter.ofPattern("d MMM yyyy"))),
        actions = Seq(
          ActionItemViewModel("site.change", routes.AmendPlanEndDateController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amendPaymentPlanConfirmation.amendPaymentPlan.endDate"))
        )
      )
    }

  def row(value: Option[LocalDate], dateFormatter: String, showChange: Boolean = false)(implicit messages: Messages): SummaryListRow = {
    val displayValue = value.map(a => a.format(DateTimeFormatter.ofPattern(dateFormatter))).getOrElse("")
    SummaryListRowViewModel(
      key   = "testOnlyAmendPaymentPlanConfirmation.endDate",
      value = ValueViewModel(displayValue),
      actions = if (showChange) {
        Seq(
          ActionItemViewModel("site.change", controllers.testonly.routes.TestOnlyAmendPlanEndDateController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("testOnlyAmendPaymentPlanConfirmation.change.endDate")),
          ActionItemViewModel("site.remove", controllers.testonly.routes.TestOnlyConfirmRemovePlanEndDateController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("testOnlyAmendPaymentPlanConfirmation.remove.endDate"))
        )
      } else {
        Seq.empty
      }
    )
  }

  def addRow()(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(messages("testOnlyAmendPaymentPlanConfirmation.endDate"))),
      value = Value(
        HtmlContent(
          s"""<a class="govuk-link" href="${routes.AmendPlanEndDateController.onPageLoad(NormalMode).url}">${messages(
              "testOnlyAmendPaymentPlanConfirmation.addPlanEndDateLink"
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
        key = "amendPaymentPlanConfirmation.amendPaymentPlan.endDate",
        ValueViewModel(dateText),
        actions = Seq.empty
      )
    )
  }
}
