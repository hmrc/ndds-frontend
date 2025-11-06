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

package views.templates

import base.SpecBase
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.templates.Layout

class LayoutSpec extends SpecBase with Matchers {

  "Layout" - {

    "must render page title" in new Setup {
      val html = layout("Test Page Title")(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.title() must include("Test Page Title")
    }

    "must render content block" in new Setup {
      val html = layout("Test Page")(Html("<p>Test content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.text() must include("Test content")
    }

    "must show back link by default" in new Setup {
      val html = layout("Test Page")(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.select("a.govuk-back-link").size() mustBe 1
    }

    "must not show back link when showBackLink is false" in new Setup {
      val html = layout("Test Page", showBackLink = false)(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.select("a.govuk-back-link").size() mustBe 0
    }

    "must include timeout dialog when timeout is true" in new Setup {
      val html = layout("Test Page", timeout = true)(Html("<p>Content</p>"))
      html.toString must include("hmrc-timeout-dialog")
    }

    "must not include timeout dialog when timeout is false" in new Setup {
      val html = layout("Test Page", timeout = false)(Html("<p>Content</p>"))
      html.toString must not include "hmrc-timeout-dialog"
    }

    "must use three-quarters grid when useThreeQuartersGrid is true" in new Setup {
      val html = layout("Test Page", useThreeQuartersGrid = true)(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-grid-column-three-quarters").size() mustBe 1
    }

    "must use two-thirds grid by default" in new Setup {
      val html = layout("Test Page")(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-grid-column-two-thirds").size() mustBe 1
    }

    "must include beta banner" in new Setup {
      val html = layout("Test Page")(Html("<p>Content</p>"))
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-phase-banner").size() mustBe 1
    }
  }

  trait Setup {
    val app: Application = applicationBuilder().build()
    implicit val request: Request[_] = FakeRequest()
    implicit val msgs: Messages = messages(app)

    val layout = app.injector.instanceOf[Layout]
  }
}

