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
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.components.BulletListLinks

class BulletListLinksSpec extends SpecBase with Matchers {

  "BulletListLink" - {
    "must render all bullet points in the output HTML when links are None" in new Setup {
      val items = Seq("First bullet" -> None, "Second bullet" -> None, "Third bullet" -> None)
      val html = bulletList(items)
      val doc = Jsoup.parse(html.body)
      val bullets = doc.select("ul li")
      bullets.size mustBe 3
      bullets.get(0).text mustBe "First bullet"
      bullets.get(1).text mustBe "Second bullet"
      bullets.get(2).text mustBe "Third bullet"
    }

    "must render all bullet points in the output HTML when links are true" in new Setup {
      val items = Seq(
        "First bullet"  -> Some(Call("GET", "/test-link")),
        "Second bullet" -> Some(Call("GET", "/test-link")),
        "Third bullet"  -> Some(Call("GET", "/test-link"))
      )
      val html = bulletList(items)
      val doc = Jsoup.parse(html.body)
      val bullets = doc.select("ul li")
      bullets.size mustBe 3
      bullets.get(0).text mustBe "First bullet(opens in new tab)"
      bullets.get(1).text mustBe "Second bullet(opens in new tab)"
      bullets.get(2).text mustBe "Third bullet(opens in new tab)"
    }

    "must render all bullet points in the output HTML when some links are true, others are None" in new Setup {
      val items = Seq(
        "First bullet"  -> Some(Call("GET", "/test-link")),
        "Second bullet" -> None,
        "Third bullet"  -> Some(Call("GET", "/test-link"))
      )
      val html = bulletList(items)
      val doc = Jsoup.parse(html.body)
      val bullets = doc.select("ul li")
      bullets.size mustBe 3
      bullets.get(0).text mustBe "First bullet(opens in new tab)"
      bullets.get(1).text mustBe "Second bullet"
      bullets.get(2).text mustBe "Third bullet(opens in new tab)"
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val bulletList = app.injector.instanceOf[BulletListLinks]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
