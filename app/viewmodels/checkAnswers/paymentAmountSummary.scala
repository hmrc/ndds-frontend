package viewmodels.checkAnswers

import config.CurrencyFormatter.currencyFormat
import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.paymentAmountPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object paymentAmountSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(paymentAmountPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "paymentAmount.checkYourAnswersLabel",
          value   = ValueViewModel(currencyFormat(answer)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.paymentAmountController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("paymentAmount.change.hidden"))
          )
        )
    }
}
