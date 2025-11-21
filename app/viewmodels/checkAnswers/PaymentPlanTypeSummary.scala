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
import pages.PaymentPlanTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PaymentPlanTypeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PaymentPlanTypePage) match {
      case Some(planType) =>
        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"checkYourAnswers.paymentPlanType.$planType"))
          )
        )

        Some(
          SummaryListRowViewModel(
            key   = "checkYourAnswers.paymentPlanType",
            value = value,
            actions = Seq(
              ActionItemViewModel("site.change", routes.PaymentPlanTypeController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("paymentPlanType.change.hidden"))
            )
          )
        )

      case None =>
        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages("checkYourAnswers.paymentPlanType.singlePaymentPlan"))
          )
        )
        Some(
          SummaryListRowViewModel(
            key     = "checkYourAnswers.paymentPlanType",
            value   = value,
            actions = Seq.empty
          )
        )
    }

  def rowNoAction(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PaymentPlanTypePage) match {
      case Some(planType) =>
        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"checkYourAnswers.paymentPlanType.$planType"))
          )
        )

        Some(
          SummaryListRowViewModel(
            key     = "checkYourAnswers.paymentPlanType",
            value   = value,
            actions = Seq.empty
          )
        )

      case None =>
        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages("checkYourAnswers.paymentPlanType.singlePaymentPlan"))
          )
        )
        Some(
          SummaryListRowViewModel(
            key     = "checkYourAnswers.paymentPlanType",
            value   = value,
            actions = Seq.empty
          )
        )
    }
}
