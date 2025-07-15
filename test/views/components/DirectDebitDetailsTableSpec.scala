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

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.DirectDebitDetailsTable
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class DirectDebitDetailsTableSpec extends SpecBase with Matchers {

  "DirectDebitDetailsTable" - {
    "must render the correct headers and rows in the output HTML" in new Setup {
      val headerLeft = "Account"
      val headerRightText = "Change"
      val headerRightUrl = "/change"
      val headerRightVisuallyHiddenText = Some("Change account details")
      val rows = Seq(
        ("Sort code", "12-34-56"),
        ("Account number", "12345678")
      )
      val html = directDebitDetailsTable.apply(
        headerLeft,
        headerRightText,
        headerRightUrl,
        headerRightVisuallyHiddenText,
        rows
      )
      val doc = Jsoup.parse(html.body)
      val headers = doc.select("thead th")
      headers.size mustBe 2
      headers.get(0).text mustBe headerLeft
      headers.get(1).text must include (headerRightText)
      headerRightVisuallyHiddenText.foreach { hidden =>
        headers.get(1).text must include (hidden)
      }
      val tableRows = doc.select("tbody tr")
      tableRows.size mustBe 2
      tableRows.get(0).text must include ("Sort code")
      tableRows.get(0).text must include ("12-34-56")
      tableRows.get(1).text must include ("Account number")
      tableRows.get(1).text must include ("12345678")
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val directDebitDetailsTable = app.injector.instanceOf[DirectDebitDetailsTable]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
