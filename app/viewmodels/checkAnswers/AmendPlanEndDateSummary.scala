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
import models.{CheckMode, UserAnswers}
import pages.AmendPlanEndDatePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AmendPlanEndDateSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AmendPlanEndDatePage).map {
      answer =>
        SummaryListRowViewModel(
          key = "amendPlanEndDate.checkYourAnswersLabel",
          value = ValueViewModel(answer.format(DateTimeFormatter.ofPattern("d MMM yyyy"))),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AmendPlanEndDateController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("amendPlanEndDate.change.hidden"))
          )
        )
    }

  def row(value: LocalDate)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "paymentPlanDetails.details.planEndDate",
      value = ValueViewModel(value.format(DateTimeFormatter.ofPattern("d MMM yyyy"))),
      actions = Seq.empty
    )
}
