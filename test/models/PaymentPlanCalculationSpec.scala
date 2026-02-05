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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.LocalDate

class PaymentPlanCalculationSpec extends AnyWordSpec with Matchers {

  "PaymentPlanCalculation JSON format" should {

    "serialize to JSON correctly" in {
      val model = PaymentPlanCalculation(
        regularPaymentAmount   = Some(BigDecimal(100.50)),
        finalPaymentAmount     = Some(BigDecimal(50.25)),
        secondPaymentDate      = Some(LocalDate.parse("2025-09-16")),
        penultimatePaymentDate = Some(LocalDate.parse("2025-12-01")),
        finalPaymentDate       = Some(LocalDate.parse("2026-01-15"))
      )

      val json = Json.toJson(model)

      (json \ "regularPaymentAmount").as[BigDecimal] shouldBe 100.50
      (json \ "finalPaymentAmount").as[BigDecimal]   shouldBe 50.25
      (json \ "secondPaymentDate").as[String]        shouldBe "2025-09-16"
      (json \ "penultimatePaymentDate").as[String]   shouldBe "2025-12-01"
      (json \ "finalPaymentDate").as[String]         shouldBe "2026-01-15"
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          | "regularPaymentAmount": 100.50,
          | "finalPaymentAmount": 50.25,
          | "secondPaymentDate": "2025-09-16",
          | "penultimatePaymentDate": "2025-12-01",
          | "finalPaymentDate": "2026-01-15"
          |}
          |""".stripMargin
      )

      val result = json.as[PaymentPlanCalculation]

      result.regularPaymentAmount   shouldBe Some(BigDecimal(100.50))
      result.finalPaymentAmount     shouldBe Some(BigDecimal(50.25))
      result.secondPaymentDate      shouldBe Some(LocalDate.parse("2025-09-16"))
      result.penultimatePaymentDate shouldBe Some(LocalDate.parse("2025-12-01"))
      result.finalPaymentDate       shouldBe Some(LocalDate.parse("2026-01-15"))
    }

    "handle missing optional fields" in {
      val json = Json.parse("{}")

      val result = json.as[PaymentPlanCalculation]

      result shouldBe PaymentPlanCalculation(None, None, None, None, None)
    }

    "support round-trip serialization and deserialization" in {
      val original = PaymentPlanCalculation(
        Some(200.00),
        None,
        Some(LocalDate.parse("2025-10-01")),
        None,
        Some(LocalDate.parse("2025-12-31"))
      )

      val json = Json.toJson(original)
      val parsed = json.as[PaymentPlanCalculation]

      parsed shouldBe original
    }
  }
}
