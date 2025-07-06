package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.AuthorisedAccountHolderPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AuthorisedAccountHolderSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AuthorisedAccountHolderPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"authorisedAccountHolder.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "authorisedAccountHolder.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.AuthorisedAccountHolderController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("authorisedAccountHolder.change.hidden"))
          )
        )
    }
}
