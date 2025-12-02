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
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.components.Paragraph

class ParagraphSpec extends SpecBase with Matchers {

  "paragraph" - {
    "must render the correct paragraph text in the output HTML" in new Setup {
      val paragraphText = "Test paragraph content."
      val html = p(paragraphText)
      val doc = Jsoup.parse(html.body)
      val para = doc.select("p")
      para.size mustBe 1
      para.text mustBe paragraphText
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val p = app.injector.instanceOf[Paragraph]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
