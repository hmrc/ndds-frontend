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
import org.scalacheck.Gen
import play.api.data.FormError

class YourBankDetailsFormProviderSpec extends StringFieldBehaviours {

  val form = new YourBankDetailsFormProvider()()

  def numericStringOfLength(n: Int): Gen[String] =
    Gen.listOfN(n, Gen.numChar).map(_.mkString)

  def numericStringWithSpacesOrHyphens(length: Int): Gen[String] =
    numericStringOfLength(length).map { s =>
      s.grouped(2).mkString("-")
    }

  ".accountHolderName" - {

    val fieldName = "accountHolderName"
    val requiredKey = "yourBankDetails.error.accountHolderName.required"
    val lengthKey = "yourBankDetails.error.accountHolderName.length"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".sortCode" - {

    val fieldName = "sortCode"
    val requiredKey = "yourBankDetails.error.sortCode.required"
    val lengthKey = "yourBankDetails.error.sortCode.length"
    val tooShortKey = "yourBankDetails.error.sortCode.tooShort"
    val numericOnlyKey = "yourBankDetails.error.sortCode.numericOnly"
    val maxLength = 6

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numericStringOfLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "not bind strings shorter than 6 characters" in {
      val result = form.bind(Map(fieldName -> "12345")).apply(fieldName)
      result.errors must contain only FormError(fieldName, tooShortKey, Seq(maxLength))
    }

    "not bind non-numeric input" in {
      val result = form.bind(Map(fieldName -> "12A456")).apply(fieldName)
      result.errors.exists(_.message == numericOnlyKey) mustBe true
    }

    "bind valid sort code with spaces or hyphens" in {
      val bound = form.bind(
        Map(
          "accountHolderName" -> "John Doe",
          "sortCode"          -> "12-34-56",
          "accountNumber"     -> "12345678"
        )
      )

      bound.errors mustBe empty
      bound.value.get.sortCode mustBe "123456"
    }

    "bind valid sort code with spaces" in {
      val bound = form.bind(
        Map(
          "accountHolderName" -> "John Doe",
          "sortCode"          -> "12 34 56",
          "accountNumber"     -> "12345678"
        )
      )

      bound.errors mustBe empty
      bound.value.get.sortCode mustBe "123456"
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".accountNumber" - {

    val fieldName = "accountNumber"
    val requiredKey = "yourBankDetails.error.accountNumber.required"
    val lengthKey = "yourBankDetails.error.accountNumber.length"
    val tooShortKey = "yourBankDetails.error.accountNumber.tooShort"
    val numericOnlyKey = "yourBankDetails.error.accountNumber.numericOnly"
    val maxLength = 8

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numericStringOfLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength   = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "not bind strings shorter than 8 characters" in {
      val result = form.bind(Map(fieldName -> "1234567")).apply(fieldName)
      result.errors must contain only FormError(fieldName, tooShortKey, Seq(maxLength))
    }

    "not bind non-numeric input" in {
      val result = form.bind(Map(fieldName -> "1234A678")).apply(fieldName)
      result.errors.exists(_.message == numericOnlyKey) mustBe true
    }

    "bind valid account number with spaces" in {
      val bound = form.bind(
        Map(
          "accountHolderName" -> "John Doe",
          "sortCode"          -> "123456",
          "accountNumber"     -> "12 34 56 78"
        )
      )

      bound.errors mustBe empty
      bound.value.get.accountNumber mustBe "12345678"
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
