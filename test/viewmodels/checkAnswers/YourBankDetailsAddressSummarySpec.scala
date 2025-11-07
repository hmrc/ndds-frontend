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

import base.SpecBase
import models.UserAnswers
import models.responses.{BankAddress, Country}
import pages.BankDetailsAddressPage
import play.api.i18n.MessagesImpl
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html

class YourBankDetailsAddressSummarySpec extends SpecBase {

  "YourBankDetailsAddressSummary" - {

    "row" - {
      "must return a SummaryListRow when UserAnswers has BankDetailsAddressPage set" in new Setup {
        val address = BankAddress(
          lines    = Seq("Line1", "Line2"),
          town     = Some("London"),
          postCode = Some("SW1A 1AA"),
          country  = Country("United Kingdom")
        )
        val userAnswers = UserAnswers("id").set(BankDetailsAddressPage, address).success.value
        val result = YourBankDetailsAddressSummary.row(userAnswers)(messages)

        result mustBe defined
        result.value.key.content.asHtml.toString must include("Bank address")

        val expectedHtml = "Line1<br/>Line2<br/>London<br/>SW1A 1AA<br/>United Kingdom"
        result.value.value.content.asHtml.toString mustEqual Html(expectedHtml).toString
      }

      "must return a SummaryListRow when UserAnswers has BankDetailsAddressPage set but town and postCode are None" in new Setup {
        val address = BankAddress(
          lines    = Seq("Line1", "Line2"),
          town     = None,
          postCode = None,
          country  = Country("United Kingdom")
        )
        val userAnswers = UserAnswers("id").set(BankDetailsAddressPage, address).success.value
        val result = YourBankDetailsAddressSummary.row(userAnswers)(messages)

        result mustBe defined
        result.value.key.content.asHtml.toString must include("Bank address")

        val expectedHtml = "Line1<br/>Line2<br/>United Kingdom"
        result.value.value.content.asHtml.toString mustEqual Html(expectedHtml).toString
      }

      "must return None when UserAnswers does not have BankDetailsAddressPage set" in new Setup {
        val result = YourBankDetailsAddressSummary.row(UserAnswers("id"))(messages)
        result mustBe None
      }
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val messages: MessagesImpl = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
