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
import views.html.components.MainButton
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class MainButtonSpec extends SpecBase with Matchers {

  "MainButton" - {
    "must render the correct button text and default class" in new Setup {
      val buttonText = "site.continue"
      val html = mainButton(buttonText)
      val doc = Jsoup.parse(html.body)
      val button = doc.select("button.govuk-button")
      button.size mustBe 1
      button.text mustBe messages(buttonText)
    }

    "must render as a link if href is provided" in new Setup {
      val buttonText = "site.save"
      val href = Some("/save-path")
      val html = mainButton(buttonText, href)
      val doc = Jsoup.parse(html.body)
      val link = doc.select("a.govuk-button")
      link.size mustBe 1
      link.attr("href") mustBe "/save-path"
      link.text mustBe messages(buttonText)
    }

    "must apply custom CSS classes if provided" in new Setup {
      val buttonText = "site.submit"
      val customClass = "my-custom-class"
      val html = mainButton(buttonText, None, customClass)
      val doc = Jsoup.parse(html.body)
      val button = doc.select(s".govuk-button.$customClass")
      button.size mustBe 1
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val mainButton = app.injector.instanceOf[MainButton]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
