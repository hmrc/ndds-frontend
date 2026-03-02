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

  val invalidKey = "paymentReference.error.invalid.sa"
  val lengthKey = "paymentReference.error.length.sa"
  val requiredKey = "paymentReference.error.required.sa"
  val maxLength = 100

  val form = new PaymentReferenceFormProvider().apply(None, None)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    "trim spaces before validating (SA)" in {
      val saForm = new PaymentReferenceFormProvider().apply(
        Some(DirectDebitSource.SA),
        Some(_ => true)
      )

      val result = saForm.bind(Map(fieldName -> " 12345678901 "))

      result.errors mustBe empty
      result.value mustBe Some("12345678901")
    }

    "return required error when value is only spaces (SA)" in {
      val saForm = new PaymentReferenceFormProvider().apply(
        Some(DirectDebitSource.SA),
        Some(_ => true)
      )

      val result = saForm.bind(Map(fieldName -> "   "))

      result.errors must contain(FormError(fieldName, requiredKey))
    }

    "return length error when trimmed value is wrong length (SA expects 11)" in {
      val saForm = new PaymentReferenceFormProvider().apply(
        Some(DirectDebitSource.SA),
        Some(_ => true)
      )

      val result = saForm.bind(Map(fieldName -> " 1234567890 "))

      result.errors must contain(FormError(fieldName, lengthKey))
    }

    behave like invalidField(
      new PaymentReferenceFormProvider().apply(Some(DirectDebitSource.SA), Some(_ => false)),
      fieldName,
      requiredError = FormError(fieldName, invalidKey),
      "12345678901"
    )
  }
}
