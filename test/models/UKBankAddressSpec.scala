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

package models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*
import play.twirl.api.Html

class UKBankAddressSpec extends AnyWordSpec with Matchers {

  "UKBankAddress JSON format" should {

    "serialize to JSON correctly" in {
      val model = UKBankAddress(
        addressLine1 = "10 Downing Street",
        addressLine2 = Some("Westminster"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        addressLine5 = None,
        postCode     = "SW1A 2AA"
      )

      val json = Json.toJson(model)

      (json \ "addressLine1").as[String] shouldBe "10 Downing Street"
      (json \ "addressLine2").as[String] shouldBe "Westminster"
      (json \ "addressLine3").as[String] shouldBe "London"
      (json \ "postCode").as[String]     shouldBe "SW1A 2AA"
      (json \ "addressLine4").toOption   shouldBe None
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          | "addressLine1": "10 Downing Street",
          | "addressLine2": "Westminster",
          | "addressLine3": "London",
          | "postCode": "SW1A 2AA"
          |}
          |""".stripMargin
      )

      val result = json.as[UKBankAddress]

      result.addressLine1 shouldBe "10 Downing Street"
      result.addressLine2 shouldBe Some("Westminster")
      result.addressLine3 shouldBe Some("London")
      result.addressLine4 shouldBe None
      result.addressLine5 shouldBe None
      result.postCode     shouldBe "SW1A 2AA"
    }

    "support round-trip JSON" in {
      val original = UKBankAddress("221B Baker Street", None, None, None, None, "NW1 6XE")
      val json = Json.toJson(original)
      val parsed = json.as[UKBankAddress]
      parsed shouldBe original
    }
  }

  "UKBankAddress HTML formatting" should {

    "render all lines including optional ones" in {
      val address = UKBankAddress(
        "10 Downing Street",
        Some("Westminster"),
        Some("London"),
        Some("Greater London"),
        Some("England"),
        "SW1A 2AA"
      )

      val html: Html = address.getFullAddress
      val rendered = html.body

      rendered should include("10 Downing Street<br>")
      rendered should include("Westminster<br>")
      rendered should include("London<br>")
      rendered should include("Greater London<br>")
      rendered should include("England<br>")
      rendered should include("SW1A 2AA<br>")
    }

    "skip missing optional lines" in {
      val address = UKBankAddress(
        "221B Baker Street",
        None,
        None,
        None,
        None,
        "NW1 6XE"
      )

      val html = address.getFullAddress.body

      html should include("221B Baker Street<br>")
      html should include("NW1 6XE<br>")
      html should not include "None"
    }

    "escape HTML in address lines" in {
      val address = UKBankAddress(
        "123 <Hack> Street",
        Some("Apartment & Co"),
        None,
        None,
        None,
        "AB1 <CD>"
      )

      val html = address.getFullAddress.body

      html should include("123 &lt;Hack&gt; Street<br>")
      html should include("Apartment &amp; Co<br>")
      html should include("AB1 &lt;CD&gt;<br>")
    }
  }
}
