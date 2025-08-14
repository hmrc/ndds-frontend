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

import models.UserAnswers
import pages.BankDetailsAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DirectDebitDetailsData
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object YourBankDetailsAddressSummary extends DirectDebitDetailsData{

  def row1(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] ={
    val formattedAddress = ukBankAddress.getFullAddress
       Some( SummaryListRowViewModel(
          key     = "bankDetailsCheckYourAnswer.account.bank.address",
          value   = ValueViewModel(HtmlContent(formattedAddress)),
          actions = Seq.empty
        ))
    }

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers
      .get(BankDetailsAddressPage)
      .map { answer =>

        val field1:      String = HtmlFormat.escape(answer.lines).toString + "<br>"
        val field2:      String = HtmlFormat.escape(answer.town).toString + "<br>"
        val field3:      String = HtmlFormat.escape(answer.postCode).toString + "<br>"
        val field4:      String = HtmlFormat.escape(answer.country.name).toString + "<br>"


        val fullAddress: String = field1 + field2 + field3 + field4

        SummaryListRowViewModel(
          key = "bankDetailsCheckYourAnswer.account.bank.address",
          value = ValueViewModel(HtmlContent(fullAddress )),
          actions = Seq.empty
        )
      }

}
