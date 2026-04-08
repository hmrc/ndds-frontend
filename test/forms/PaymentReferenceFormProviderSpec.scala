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

  // Updated valid dummy values per current provider
  private val dummyValues: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT   -> "8337018376A00108A",
    DirectDebitSource.MGD  -> "XVM00005554321",
    DirectDebitSource.NIC  -> "600340016213526259",
    DirectDebitSource.PAYE -> "123PA12345678", // 3 digits + P + 1 letter + 7 digits + optional digit (13 chars)
    DirectDebitSource.SA   -> "1234567890K",
    DirectDebitSource.SDLT -> "123456789MA", // 9 digits + M + letter
    DirectDebitSource.TC   -> "AB123456789012NA", // 2 letters + 12 digits + N + letter
    DirectDebitSource.VAT  -> "123456789", // 9+ digits only
    DirectDebitSource.OL   -> "XAB12345678901" // matches OL regex; 3rd char != 'M'
  )

  // Updated valid dummy values per current provider
  private val dummyValuesWithMidSpaces: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT   -> "8337018 376A00108A",
    DirectDebitSource.MGD  -> "XVM000 05554321",
    DirectDebitSource.NIC  -> "600340 016213526259",
    DirectDebitSource.PAYE -> "123PA 12345678", // 3 digits + P + 1 letter + 7 digits + optional digit (13 chars)
    DirectDebitSource.SA   -> "123456 7890K",
    DirectDebitSource.SDLT -> "1234 56789MA", // 9 digits + M + letter
    DirectDebitSource.TC   -> "AB1234 56789012NA", // 2 letters + 12 digits + N + letter
    DirectDebitSource.VAT  -> "12345 6789", // 9+ digits only
    DirectDebitSource.OL   -> "XAB123 45678901" // matches OL regex; 3rd char != 'M'
  )

  private def form(
    source: DirectDebitSource,
    validator: String => Boolean = _ => true
  ) =
    new PaymentReferenceFormProvider()
      .apply(Some(source), Some(validator))

  sources.foreach { source =>

    s".value (${source.toString})" - {

      val baseKey = if (source == DirectDebitSource.OL) "otherLiability" else source.toString.toLowerCase
      val requiredKey = s"paymentReference.$baseKey.required"
      val invalidCharsKey = s"paymentReference.$baseKey.invalidCharacters"
      val invalidFormatKey = s"paymentReference.$baseKey.invalidFormat"
      val invalidKey = s"paymentReference.$baseKey.invalid"

      "trim spaces at ends before validating" in {
        val result =
          form(source).bind(Map(fieldName -> s"  ${dummyValues(source)}  "))
        result.errors mustBe empty
        result.value mustBe Some(dummyValues(source))
      }

      "trim spaces inside before validating" in {
        val result =
          form(source).bind(Map(fieldName -> s" ${dummyValuesWithMidSpaces(source)}"))
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
          case DirectDebitSource.CT   => "8337018376B00108A"
          case DirectDebitSource.MGD  => "XAM100005554321"
          case DirectDebitSource.NIC  => "611234567890123458"
          case DirectDebitSource.PAYE => "123ABC1234567"
          case DirectDebitSource.SA   => "1234567891L"
          case DirectDebitSource.SDLT => "123456789DA"
          case DirectDebitSource.TC   => "AB123456789012ZZ"
          case DirectDebitSource.VAT  => "12345678"
          case DirectDebitSource.OL   => "XAM12345678901"
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
