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

package views.helpers

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.mvc.Call

class DirectDebitCountBackLinkSpec extends SpecBase with Matchers {

  "DirectDebitCountBackLink" - {

    val dummyCall = Call("GET", "/test-url")

    "should return Some(call.url) and show=true when count is not zero" in {
      val count = 1
      DirectDebitCountBackLink.link(count, dummyCall) shouldBe Some("/test-url")
      DirectDebitCountBackLink.show(count)            shouldBe true
    }

    "should return None and show=false when count is zero" in {
      val count = 0
      DirectDebitCountBackLink.link(count, dummyCall) shouldBe None
      DirectDebitCountBackLink.show(count)            shouldBe false
    }
  }
}
