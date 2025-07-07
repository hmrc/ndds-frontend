package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.paymentReferencePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object paymentReferenceSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(paymentReferencePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "paymentReference.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.paymentReferenceController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("paymentReference.change.hidden"))
          )
        )
    }
}
