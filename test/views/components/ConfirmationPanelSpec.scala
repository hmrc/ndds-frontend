/*
 * Copyright 2026 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.components.ConfirmationPanel

class ConfirmationPanelSpec extends SpecBase with Matchers {

  "ConfirmationPanel" - {
    "must render the correct title and body in the output HTML" in new Setup {
      val title = "Test Confirmation Title"
      val body = Html("<span>Test confirmation body.</span>")
      val html = confirmationPanel(title, body)
      val doc = Jsoup.parse(html.body)
      val panel = doc.select(".govuk-panel")
      panel.size mustBe 1
      panel.select(".govuk-panel__title").text mustBe title
      panel.select(".govuk-panel__body").html must include("<span>Test confirmation body.</span>")
    }

    "must apply custom CSS classes if provided" in new Setup {
      val title = "Title with class"
      val body = Html("Body with class")
      val customClass = "my-custom-class"
      val html = confirmationPanel(title, body, customClass)
      val doc = Jsoup.parse(html.body)
      val panel = doc.select(s".govuk-panel.$customClass")
      panel.size mustBe 1
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val confirmationPanel = app.injector.instanceOf[ConfirmationPanel]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
