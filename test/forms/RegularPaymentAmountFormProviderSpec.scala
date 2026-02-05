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

import config.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class RegularPaymentAmountFormProviderSpec extends CurrencyFieldBehaviours {

  private val form = new RegularPaymentAmountFormProvider()()

  private val fieldName = "value"
  private val minimum = 1
  private val maximum = 20000000

  private val requiredError = "regularPaymentAmount.error.required"
  private val nonNumericError = "regularPaymentAmount.error.nonNumeric"
  private val invalidNumericError = "regularPaymentAmount.error.invalidNumeric"
  private val aboveMaximumError = "regularPaymentAmount.error.aboveMaximum"
  private val belowMinimumError = "regularPaymentAmount.error.belowMinimum"

  private val validDataGenerator: Gen[String] =
    Gen
      .choose[BigDecimal](minimum, maximum)
      .map(_.setScale(2, RoundingMode.HALF_UP))
      .map(_.toString)

  ".value" - {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    "fail to bind non-numeric input with non-numeric error" in {
      val result = form.bind(Map(fieldName -> "abc"))
      result.errors must contain only FormError(fieldName, nonNumericError)
    }

    "fail to bind incorrectly formatted numeric input (e.g. one decimal place)" in {
      val result = form.bind(Map(fieldName -> "123.4"))
      result.errors must contain only FormError(fieldName, invalidNumericError)
    }

    "fail when above maximum" in {
      val result = form.bind(Map(fieldName -> (maximum + 1).toString))
      result.errors must contain only FormError(fieldName, aboveMaximumError, Seq(currencyFormat(maximum)))
    }

    "fail when below minimum" in {
      val result = form.bind(Map(fieldName -> (minimum - 1).toString))
      result.errors must contain only FormError(fieldName, belowMinimumError, Seq(currencyFormat(minimum)))
    }

    "fail when empty" in {
      val result = form.bind(Map(fieldName -> ""))
      result.errors must contain only FormError(fieldName, requiredError)
    }

    "successfully bind a whole number and format it to 2 decimal places" in {
      val result = form.bind(Map(fieldName -> "123"))
      result.value.value mustBe BigDecimal("123.00")
    }

    "successfully bind a number with exactly 2 decimal places" in {
      val result = form.bind(Map(fieldName -> "123.45"))
      result.value.value mustBe BigDecimal("123.45")
    }
  }
}
