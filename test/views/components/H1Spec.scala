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
import views.html.components.h1
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class H1Spec extends SpecBase with Matchers {

  "h1" - {
    "must render the correct heading text in the output HTML" in new Setup {
      val headingText = "Test Heading"
      val html = h1(headingText)
      val doc = Jsoup.parse(html.body)
      val heading = doc.select("h1")
      heading.size mustBe 1
      heading.text mustBe headingText
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val h1 = app.injector.instanceOf[views.html.components.h1]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
