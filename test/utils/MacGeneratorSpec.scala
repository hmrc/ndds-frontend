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

package utils

import config.FrontendAppConfig
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MacGeneratorSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "MacGenerator" should {

    "generate a stable HMAC-SHA1 MAC for known inputs" in {
      // Mock AppConfig
      val mockAppConfig = mock[FrontendAppConfig]
      when(mockAppConfig.macKey).thenReturn("CJAluZLJtC6oBLs2pqKg3A==") // Base64 key

      val macGenerator = new MacGenerator(mockAppConfig)

      val accountName = "John Doe"
      val accountNumber = "12345678"
      val sortCode = "112233"
      val lines = Seq("10 Downing Street", "Flat 2")
      val town = "London"
      val postcode = "SW1A 2AA"
      val bankName = "Bank of Scala"
      val bacsNumber = "111222"

      val mac = macGenerator.generateMac(
        accountName,
        accountNumber,
        sortCode,
        lines,
        Some(town),
        Some(postcode),
        bankName,
        bacsNumber
      )

      val expectedMac = "LTkYQNkusGfcQorgX0bOkZMATjg="
      mac must not be empty
      mac mustEqual expectedMac
    }

    "produce different MACs if any input changes" in {
      val mockAppConfig = mock[FrontendAppConfig]
      when(mockAppConfig.macKey).thenReturn("CJAluZLJtC6oBLs2pqKg3A==")

      val macGenerator = new MacGenerator(mockAppConfig)

      val baseMac = macGenerator.generateMac(
        "John Doe",
        "12345678",
        "112233",
        Seq("10 Downing Street", "Flat 2"),
        Some("London"),
        Some("SW1A 2AA"),
        "Bank of Scala",
        "111222"
      )

      val changedMac = macGenerator.generateMac(
        "Jane Doe",
        "12345678",
        "112233",
        Seq("10 Downing Street", "Flat 2"),
        Some("London"),
        Some("SW1A 2AA"),
        "Bank of Scala",
        "111222"
      )

      baseMac must not equal changedMac
    }
  }
}
