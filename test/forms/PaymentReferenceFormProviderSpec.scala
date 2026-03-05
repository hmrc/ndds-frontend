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

package forms

import forms.behaviours.StringFieldBehaviours
import models.DirectDebitSource
import play.api.data.FormError

class PaymentReferenceFormProviderSpec extends StringFieldBehaviours {

  val fieldName = "value"

  private val sources = Seq(
    DirectDebitSource.CT,
    DirectDebitSource.MGD,
    DirectDebitSource.NIC,
    DirectDebitSource.PAYE,
    DirectDebitSource.SA,
    DirectDebitSource.SDLT,
    DirectDebitSource.TC,
    DirectDebitSource.VAT,
    DirectDebitSource.OL
  )

  // Valid dummy values
  private val dummyValues: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT   -> "8337018376A00108A", // 10 digits + A001 + 2 digits + A
    DirectDebitSource.MGD  -> "XVM00005554321", // X + uppercase + M0000 + 7 digits
    DirectDebitSource.NIC  -> "600340016213526259", // starts 60 + 16 digits + last digit
    DirectDebitSource.PAYE -> "ABC1234567890", // 13–14 chars
    DirectDebitSource.SA   -> "1234567890K", // 10 digits + K
    DirectDebitSource.SDLT -> "123456789C", // 9 digits + 1 letter
    DirectDebitSource.TC   -> "AB123456789012CD", // 16 alphanum
    DirectDebitSource.VAT  -> "12345678A", // 9 alphanum
    DirectDebitSource.OL   -> "12345678901234" // 14–15 alphanum
  )

  private def form(
    source: DirectDebitSource,
    validator: String => Boolean = _ => true
  ) =
    new PaymentReferenceFormProvider()
      .apply(Some(source), Some(validator))

  sources.foreach { source =>

    s".value (${source.toString})" - {

      val baseKey = source match {
        case DirectDebitSource.OL => "otherLiability"
        case _                    => source.toString.toLowerCase
      }
      val requiredKey = s"paymentReference.$baseKey.required"
      val invalidCharsKey = s"paymentReference.$baseKey.invalidCharacters"
      val invalidFormatKey = s"paymentReference.$baseKey.invalidFormat"
      val invalidKey = s"paymentReference.$baseKey.invalid"

      "trim spaces before validating" in {
        val result =
          form(source).bind(Map(fieldName -> s"  ${dummyValues(source)}  "))
        result.errors mustBe empty
        result.value mustBe Some(dummyValues(source))
      }

      "return required when blank" in {
        val result =
          form(source).bind(Map(fieldName -> "   "))
        result.errors must contain(FormError(fieldName, requiredKey))
        result.errors.size mustBe 1
      }

      "return invalidCharacters when non-alphanumeric" in {
        val result =
          form(source).bind(Map(fieldName -> "ABC-123"))
        result.errors must contain(FormError(fieldName, invalidCharsKey))
        result.errors.size mustBe 1
      }

      "return invalidFormat when valid characters but wrong pattern" in {
        val invalidFormatValue = source match {
          case DirectDebitSource.CT   => "8337018376B99108B" // valid chars, fails A001 pattern
          case DirectDebitSource.MGD  => "XBM00001234567" // valid chars, second char wrong for MGD
          case DirectDebitSource.NIC  => "601234567890123457" // valid chars, last digit wrong for NIC
          case DirectDebitSource.PAYE => "123456789012" // too short, valid chars
          case DirectDebitSource.SA   => "1234567891K" // valid digits, pattern fails
          case DirectDebitSource.SDLT => "123456780A" // valid alphanum, pattern wrong
          case DirectDebitSource.TC   => "AB123456789012CE" // valid alphanum, pattern wrong
          case DirectDebitSource.VAT  => "12345678B" // 9 alphanum, pattern wrong
          case DirectDebitSource.OL   => "123456789012345" // 15 chars, pattern wrong if max 14
        }
        val result = form(source).bind(Map(fieldName -> invalidFormatValue))
        result.errors must contain(FormError(fieldName, invalidFormatKey))
        result.errors.size mustBe 1
      }

      source match {
        case DirectDebitSource.SA | DirectDebitSource.CT | DirectDebitSource.VAT =>
          "return invalid when validator fails" in {
            val result =
              form(source, _ => false)
                .bind(Map(fieldName -> dummyValues(source)))
            result.errors must contain(FormError(fieldName, invalidKey))
          }

          "bind successfully when all checks pass" in {
            val result =
              form(source, _ => true)
                .bind(Map(fieldName -> dummyValues(source)))
            result.errors mustBe empty
          }

        case _ =>
          "bind successfully with mock validator" in {
            val result =
              form(source, _ => true)
                .bind(Map(fieldName -> dummyValues(source)))
            result.errors mustBe empty
          }
      }

      "error precedence: required blocks others" in {
        val result =
          form(source, _ => false)
            .bind(Map(fieldName -> ""))
        result.errors.map(_.message) mustBe Seq(requiredKey)
      }

      "error precedence: invalidCharacters blocks format" in {
        val result =
          form(source, _ => false)
            .bind(Map(fieldName -> "ABC-123"))
        result.errors.map(_.message) mustBe Seq(invalidCharsKey)
      }
    }
  }
}
