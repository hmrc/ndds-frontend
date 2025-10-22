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

package views.components

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.Link
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class LinkSpec extends SpecBase with Matchers {

  "link" - {
    "must render the correct link text in the output HTML" in new Setup {
      val html = link(linkText, linkUrl, prefixTextKey = prefixText)
      val linkElement = getLinkElement(html)

      linkElement.size mustBe 1
      linkElement.text mustBe prefixText + " " + linkText
    }

    "must render the correct link text in the output HTML with an empty prefixText" in new Setup {
      val html = link(linkText, linkUrl)
      val linkElement = getLinkElement(html)

      linkElement.size mustBe 1
      linkElement.text mustBe linkText
    }

    "must render with default class when extraClasses are empty" in new Setup {
      val html = link(linkText, linkUrl, prefixTextKey = prefixText, extraClasses = "")
      val linkElement = getLinkElement(html)
      val classes = linkElement.attr("class")

      classes.trim mustBe s"govuk-body"
    }

    "must render with default and extra classes when extraClasses are non-empty" in new Setup {
      val html = link(linkText, linkUrl, prefixTextKey = prefixText, extraClasses = extraClasses)
      val linkElement = getLinkElement(html)
      val classes = linkElement.attr("class")

      classes.trim mustBe s"govuk-body $extraClasses"
    }

    "must render the correct link URL in the output HTML" in new Setup {
      val html = link(linkText, linkUrl, prefixTextKey = prefixText)
      val linkRefElement: Elements = getLinkRefElement(html)

      linkRefElement.attr("href") mustBe linkUrl
    }

    "must render the output HTML with no rel and targets attributes if isNewTab is false" in new Setup {
      val html = link(linkText, linkUrl)
      val linkRefElement: Elements = getLinkRefElement(html)

      linkRefElement.hasAttr("rel") mustBe false
      linkRefElement.hasAttr("target") mustBe false
    }

    "must render the output HTML with rel and targets attributes if isNewTab is true" in new Setup {
      val html = link(linkText, linkUrl, true, prefixTextKey = prefixText)
      val linkRefElement: Elements = getLinkRefElement(html)

      linkRefElement.hasAttr("rel") mustBe true
      linkRefElement.hasAttr("target") mustBe true

      linkRefElement.attr("rel")    must include("noreferrer noopener")
      linkRefElement.attr("target") must include("_blank")
    }

  }

  trait Setup {
    val app = applicationBuilder().build()
    val link = app.injector.instanceOf[Link]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val linkText = "link text"
    val linkUrl = "https://www.gov.uk/find-hmrc-contacts/technical-support-with-hmrc-online-services"
    val prefixText = "Jump to"
    val extraClasses = "govuk-link--inverse govuk-link--no-underline"

    def getLinkElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("p")
    }

    def getLinkRefElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("a")
    }

  }

}
