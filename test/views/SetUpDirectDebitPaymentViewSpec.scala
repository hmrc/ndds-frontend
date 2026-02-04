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

package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.should
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Request}
import play.api.test.FakeRequest
import views.html.SetupDirectDebitPaymentView

class SetUpDirectDebitPaymentViewSpec extends SpecBase with Matchers {

  "SetupDirectDebitPaymentView" - {
    "should correct title and content" in new Setup {
      titleShouldBeCorrect(view)
      shouldDisplayCorrectHeader(view)
      shouldDisplayCorrectParagraphs(view)
      shouldDisplayStartButton(view)
      shouldDisplayCorrectDropDownTitle(view)
      shouldDisplayCorrectDropDownParapgraphs(view)
      shouldDisplayCorrectDropDownLinkTitles(view)
      shouldShowBackLink(view)
    }
  }

  private def shouldShowBackLink(view: Document) = {
    view.select("a.govuk-back-link").size() mustBe 1
  }

  private def titleShouldBeCorrect(view: Document)(implicit msgs: Messages) = {
    view.title() must include(msgs("setupDirectDebitPayment.title"))
  }

  private def shouldDisplayCorrectHeader(view: Document)(implicit msgs: Messages) = {
    view.select("h1").text() mustBe msgs("setupDirectDebitPayment.heading")
  }

  private def shouldDisplayCorrectParagraphs(view: Document)(implicit msgs: Messages) = {
    val text = view.text()

    text must include(msgs("setUpDirectDebitPayment.p1"))
    text must include(msgs("setUpDirectDebitPayment.p2"))
    text must include(msgs("setUpDirectDebitPayment.p3"))
  }

  private def shouldDisplayCorrectDropDownTitle(view: Document)(implicit msgs: Messages) = {
    view.select("#dd-more-info summary").text() mustBe msgs("setUpDirectDebitPayment.collapsable")
  }

  private def shouldDisplayCorrectDropDownParapgraphs(view: Document)(implicit msgs: Messages) = {
    val text = view.select("#dd-more-info p").text()

    text must include(msgs("setUpDirectDebitPayment.more.p1"))
    text must include(msgs("setUpDirectDebitPayment.more.p2"))
    text must include(msgs("setUpDirectDebitPayment.more.p3"))
  }

  private def shouldDisplayCorrectDropDownLinkTitles(view: Document)(implicit msgs: Messages) = {
    val anchors = view.select("#dd-more-info a").text()

    anchors must include(msgs("setUpDirectDebitPayment.more.bullet.l1"))
    anchors must include(msgs("setUpDirectDebitPayment.more.bullet.l2"))
    anchors must include(msgs("setUpDirectDebitPayment.more.bullet.l3"))
    anchors must include(msgs("setUpDirectDebitPayment.more.bullet.l4"))
  }

  private def shouldDisplayStartButton(view: Document)(implicit msgs: Messages) = {
    val button = view.select("#start-button")
    button.size() mustBe 1
    button.text() mustBe msgs("setupDirectDebitPayment.startNow")
  }

  trait Setup {
    val app: Application = applicationBuilder().build()
    implicit val request: Request[AnyContent] = FakeRequest()
    implicit val msgs: Messages = messages(app)

    private val viewTemplate = app.injector.instanceOf[SetupDirectDebitPaymentView]

    val directDebitCount = 1
    val backCall: Call = Call("GET", "/previous")

    val html = viewTemplate(directDebitCount, backCall).body
    val view: Document = Jsoup.parse(html)
  }
}
