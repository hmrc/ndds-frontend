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
import views.html.components.FieldSet
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup
import play.twirl.api.Html

class FieldSetSpec extends SpecBase with Matchers {

  "FieldSet" - {
    "must render with correct legend text" in new Setup {
      val legendText = "Test Legend"
      val html       = fieldSet(legendText, asHeading = true, describedBy = None)(testContent)
      val legend     = getLegendElement(html)
      legend.size mustBe 1
      legend.text mustBe legendText
    }

    "must render with heading styling when asHeading is true" in new Setup {
      val html      = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val legend    = getLegendElement(html)
      val classAttr = legend.attr("class")
      classAttr must include("govuk-fieldset__legend--l")
    }

    "must not render with heading styling when asHeading is false" in new Setup {
      val html      = fieldSet("Test Legend", asHeading = false, describedBy = None)(testContent)
      val legend    = getLegendElement(html)
      val classAttr = legend.attr("class")
      classAttr must not include "govuk-fieldset__legend--l"
    }

    "must render with isPageHeading attribute when asHeading is true" in new Setup {
      val html   = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val legend = getLegendElement(html)
      legend.attr("class") must include("govuk-fieldset__legend--l")
    }

    "must not render with isPageHeading attribute when asHeading is false" in new Setup {
      val html   = fieldSet("Test Legend", asHeading = false, describedBy = None)(testContent)
      val legend = getLegendElement(html)
      legend.attr("class") must not include "govuk-fieldset__legend--l"
    }

    "must render with describedBy attribute when provided" in new Setup {
      val describedBy = "test-description"
      val html        = fieldSet("Test Legend", asHeading = true, describedBy = Some(describedBy))(testContent)
      val fieldset    = getFieldsetElement(html)
      fieldset.attr("aria-describedby") mustBe describedBy
    }

    "must not render with describedBy attribute when not provided" in new Setup {
      val html     = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val fieldset = getFieldsetElement(html)
      fieldset.hasAttr("aria-describedby") mustBe false
    }

    "must render with correct content" in new Setup {
      val html     = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val fieldset = getFieldsetElement(html)
      fieldset.html must include(testContent.body)
    }

    "must render with fieldset element" in new Setup {
      val html     = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val fieldset = getFieldsetElement(html)
      fieldset.size mustBe 1
    }

    "must render with legend element" in new Setup {
      val html   = fieldSet("Test Legend", asHeading = true, describedBy = None)(testContent)
      val legend = getLegendElement(html)
      legend.size mustBe 1
    }

  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val fieldSet                                  = app.injector.instanceOf[FieldSet]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val testContent = Html("<p>Test content</p>")

    def getFieldsetElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("fieldset")
    }

    def getLegendElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("legend")
    }
  }
}
