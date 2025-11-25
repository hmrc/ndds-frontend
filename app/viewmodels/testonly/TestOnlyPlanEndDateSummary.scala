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

package viewmodels.testonly

import controllers.routes
import models.NormalMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TestOnlyPlanEndDateSummary {

  def row(endDate: LocalDate)(implicit messages: Messages): SummaryListRow = {

    val formatted =
      endDate.format(DateTimeFormatter.ofPattern(Constants.shortDateTimeFormatPattern))

    SummaryListRowViewModel(
      key   = "testOnlyAmendingPaymentPlan.budgetPlanEndDate",
      value = ValueViewModel(formatted),
      actions = Seq(
        ActionItemViewModel("site.change", routes.AmendPlanEndDateController.onPageLoad(NormalMode).url)
          .withVisuallyHiddenText(messages("testOnlyAmendingPaymentPlan.budgetPlanEndDate.change.hidden")),
        ActionItemViewModel("site.remove", routes.RemovingThisSuspensionController.onPageLoad(NormalMode).url)
          .withVisuallyHiddenText(messages("testOnlyAmendingPaymentPlan.budgetPlanEndDate.remove.hidden"))
      )
    )
  }

  def addRow()(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(Text(messages("testOnlyAmendingPaymentPlan.budgetPlanEndDate"))),
      value = Value(
        HtmlContent(
          s"""<a class="govuk-link" href="${routes.AmendPlanEndDateController.onPageLoad(NormalMode).url}">${messages(
              "testOnlyAmendingPaymentPlan.budgetPlanEndDate.addPlanEndDateLink"
            )}</a>"""
        )
      ),
      actions = None
    )
}
