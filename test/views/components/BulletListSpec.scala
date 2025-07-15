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
import views.html.components.BulletList
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class BulletListSpec extends SpecBase with Matchers {

  "BulletList" - {
    "must render all bullet points in the output HTML" in new Setup {
      val items = Seq("First bullet", "Second bullet", "Third bullet")
      val html = bulletList(items)
      val doc = Jsoup.parse(html.body)
      val bullets = doc.select("ul li")
      bullets.size mustBe 3
      bullets.get(0).text mustBe "First bullet"
      bullets.get(1).text mustBe "Second bullet"
      bullets.get(2).text mustBe "Third bullet"
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val bulletList = app.injector.instanceOf[BulletList]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
