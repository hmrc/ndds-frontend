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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class DirectDebitDetailsSpec extends AnyWordSpec with Matchers {

  "DirectDebitDetails" should {

    "serialize to JSON correctly" in {
      val details = DirectDebitDetails(
        directDebitReference = "DD123456789",
        setupDate            = "2025-08-10",
        sortCode             = "12-34-56",
        accountNumber        = "12345678",
        paymentPlans         = "PlanA"
      )

      val json = Json.toJson(details)

      json.toString must include("DD123456789")
      json.toString must include("2025-08-10")
      json.toString must include("12-34-56")
      json.toString must include("12345678")
      json.toString must include("PlanA")
    }

    "deserialize from JSON correctly" in {
      val jsonString =
        """
          |{
          |  "directDebitReference": "DD987654321",
          |  "setupDate": "2025-08-11",
          |  "sortCode": "65-43-21",
          |  "accountNumber": "87654321",
          |  "paymentPlans": "PlanB"
          |}
          |""".stripMargin

      val json = Json.parse(jsonString)
      val details = json.as[DirectDebitDetails]

      details.directDebitReference mustBe "DD987654321"
      details.setupDate mustBe "2025-08-11"
      details.sortCode mustBe "65-43-21"
      details.accountNumber mustBe "87654321"
      details.paymentPlans mustBe "PlanB"
    }
  }
}
