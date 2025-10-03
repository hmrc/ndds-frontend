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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AmendSuspendDateSummary  {

  def row(suspendDate: Option[LocalDate], isStartDate: Boolean)(implicit messages: Messages): SummaryListRow =
    val label = if(isStartDate) {
      "paymentPlanDetails.details.suspendStartDate"
    } else {
      "paymentPlanDetails.details.suspendEndDate"
    }
    val formattedDate = suspendDate
      .map(_.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
      .getOrElse("")
    SummaryListRowViewModel(
      key = label,
      value = ValueViewModel(formattedDate),
      actions = Seq.empty
    )

}
