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

class YourBankDetailsSpec extends AnyWordSpec with Matchers {

  "YourBankDetails JSON format" should {

    val sample = YourBankDetails(
      accountHolderName = "John Doe",
      sortCode = "12-34-56",
      accountNumber = "12345678"
    )

    val json = Json.parse(
      """
        |{
        |  "accountHolderName": "John Doe",
        |  "sortCode": "12-34-56",
        |  "accountNumber": "12345678"
        |}
        |""".stripMargin)

    "serialize YourBankDetails to JSON" in {
      Json.toJson(sample) mustEqual json
    }

    "deserialize JSON to YourBankDetails" in {
      json.as[YourBankDetails] mustEqual sample
    }
  }
}
