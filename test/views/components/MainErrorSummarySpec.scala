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
import play.api.data.{Form, Forms}
import play.api.data.Forms.*
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup
import views.html.components.MainErrorSummary
import uk.gov.hmrc.govukfrontend.views.html.components.GovukErrorSummary

class MainErrorSummarySpec extends SpecBase with Matchers {

  "MainErrorSummary" - {
    "must render the error summary when the form has errors" in new Setup {
      val formWithError = form.bind(Map("value" -> ""))
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
    }

    "must not render the error summary when the form has no errors" in new Setup {
      val formNoError = form.bind(Map("value" -> "something"))
      val html = mainErrorSummary(formNoError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 0
    }

    "must render the error summary with error message arguments" in new Setup {
      val formWithError = form.withError(
        key     = "fieldName",
        message = "error.with.args",
        args    = Seq("arg1", "arg2")
      )
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
      doc.select(".govuk-error-summary__list a").text mustBe messages("error.with.args", "arg1", "arg2")
    }

    "must render the error summary with no href when error key is empty" in new Setup {
      val formWithError = form.withError(
        key     = "",
        message = "error.without.key"
      )
      val html = mainErrorSummary(formWithError)
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text mustBe messages("error.summary.title")
      doc.select(".govuk-error-summary__list a").isEmpty mustBe true
      doc.select(".govuk-error-summary__list li").text mustBe messages("error.without.key")
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val govukErrorSummary = app.injector.instanceOf[GovukErrorSummary]
    val mainErrorSummary = new MainErrorSummary(govukErrorSummary)
    val form: Form[String] = Form(
      "value" -> nonEmptyText
    )
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
