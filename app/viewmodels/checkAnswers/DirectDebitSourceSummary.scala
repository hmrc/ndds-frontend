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

import pages.DirectDebitSourcePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import controllers.routes
import models.{CheckMode, UserAnswers}

object DirectDebitSourceSummary {

  def row(answers: UserAnswers, showChange: Boolean = false)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DirectDebitSourcePage).map { answer =>

      val displayValue = messages(s"directDebitSource.${answer.toString}")

      SummaryListRowViewModel(
        key   = "directDebitPaymentSummary.activePayment.summary.paymentFor",
        value = ValueViewModel(Text(displayValue)),
        actions = if (showChange) {
          Seq(
            ActionItemViewModel(
              content = "site.change",
              href    = routes.DirectDebitSourceController.onPageLoad(CheckMode).url
            ).withVisuallyHiddenText(messages(""))
          )
        } else {
          Seq.empty
        }
      )
    }
}