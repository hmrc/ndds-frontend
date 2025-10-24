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
import models.{CheckMode, SuspensionPeriodRange, UserAnswers}
import pages.SuspensionPeriodRangeDatePage
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DateTimeFormats.formattedDateTimeShort
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object SuspensionPeriodRangeDateSummary {
  def row(answers: UserAnswers, showChange: Boolean = false)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SuspensionPeriodRangeDatePage).map { answer =>

      implicit val lang: Lang = messages.lang

      val formattedValue =
        s"${formattedDateTimeShort(answer.startDate.toString)} ${messages("suspensionPeriodRangeDate.to")} ${formattedDateTimeShort(answer.endDate.toString)}"
      SummaryListRowViewModel(
        key   = "suspensionPeriodRangeDate.checkYourAnswersLabel",
        value = ValueViewModel(formattedValue),
        actions = if (showChange) {
          Seq(
            ActionItemViewModel("site.change", routes.SuspensionPeriodRangeDateController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("suspensionPeriodRangeDate.change.hidden"))
          )
        } else {
          Seq.empty
        }
      )
    }
}
