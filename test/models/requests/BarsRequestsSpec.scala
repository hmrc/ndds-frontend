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

package models.requests

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class BarsRequestsSpec extends AnyWordSpec with Matchers {

  "BarsAccount" should {
    "serialize and deserialize correctly" in {
      val account = BarsAccount("123456", "12345678")
      val json = Json.toJson(account)
      (json \ "sortCode").as[String]      shouldBe "123456"
      (json \ "accountNumber").as[String] shouldBe "12345678"
      json.as[BarsAccount]                shouldBe account
    }
  }

  "BarsSubject" should {
    "serialize and deserialize correctly" in {
      val subject = BarsSubject("John Doe")
      val json = Json.toJson(subject)
      (json \ "name").as[String] shouldBe "John Doe"
      json.as[BarsSubject]       shouldBe subject
    }
  }

  "BarsBusiness" should {
    "serialize and deserialize correctly" in {
      val business = BarsBusiness("Test Company Ltd")
      val json = Json.toJson(business)
      (json \ "companyName").as[String] shouldBe "Test Company Ltd"
      json.as[BarsBusiness]             shouldBe business
    }
  }

  "BarsPersonalRequest" should {
    "serialize and deserialize correctly" in {
      val request = BarsPersonalRequest(
        BarsAccount("123456", "12345678"),
        BarsSubject("Jane Doe")
      )

      val json = Json.toJson(request)
      (json \ "account" \ "sortCode").as[String]      shouldBe "123456"
      (json \ "account" \ "accountNumber").as[String] shouldBe "12345678"
      (json \ "subject" \ "name").as[String]          shouldBe "Jane Doe"

      json.as[BarsPersonalRequest] shouldBe request
    }
  }

  "BarsBusinessRequest" should {
    "serialize and deserialize correctly" in {
      val request = BarsBusinessRequest(
        BarsAccount("654321", "87654321"),
        BarsBusiness("Widgets Inc")
      )

      val json = Json.toJson(request)
      (json \ "account" \ "sortCode").as[String]      shouldBe "654321"
      (json \ "account" \ "accountNumber").as[String] shouldBe "87654321"
      (json \ "business" \ "companyName").as[String]  shouldBe "Widgets Inc"

      json.as[BarsBusinessRequest] shouldBe request
    }
  }
}
