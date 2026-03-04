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

  private val dummyValues: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.SA   -> "12345678901",
    DirectDebitSource.CT   -> "12345678901234567",
    DirectDebitSource.VAT  -> "123456789",
    DirectDebitSource.MGD  -> "A1234567890123",
    DirectDebitSource.NIC  -> "601234567890123456",
    DirectDebitSource.PAYE -> "1234567890123",
    DirectDebitSource.SDLT -> "12345678901",
    DirectDebitSource.TC   -> "AB123456789012CD",
    DirectDebitSource.OL   -> "12345678901234"
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

      "return invalidFormat when length clearly wrong" in {
        val result =
          form(source).bind(Map(fieldName -> "123"))
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
