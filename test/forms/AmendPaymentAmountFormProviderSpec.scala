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

package forms

import forms.amend.AmendPaymentAmountFormProvider
import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class AmendPaymentAmountFormProviderSpec extends CurrencyFieldBehaviours {

  val provider = new AmendPaymentAmountFormProvider()
  private val form = provider()
  private val min = provider.MIN_AMOUNT
  private val max = provider.MAX_AMOUNT

  private val fieldName = "value"

  private val validDataGenerator: Gen[String] =
    Gen.choose[BigDecimal](min, max)
      .map(_.setScale(2, RoundingMode.HALF_UP))
      .map(_.toString)

  ".value" - {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "paymentAmount.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "paymentAmount.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      max,
      FormError(fieldName, "paymentAmount.error.max.min.range", Seq(max))
    )

    "fail when below minimum" in {
      val result = form.bind(Map(fieldName -> (min - 0.01).toString))
      result.errors must contain only FormError(fieldName, "paymentAmount.error.max.min.range", Seq(min))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "paymentAmount.error.required")
    )

    "bind a whole number and format it as .00" in {
      val result = form.bind(Map(fieldName -> "1"))
      result.errors mustBe empty
      result.value.value mustBe BigDecimal("1.00")
    }

    "bind a value with exactly two decimal places" in {
      val result = form.bind(Map(fieldName -> "123.45"))
      result.errors mustBe empty
      result.value.value mustBe BigDecimal("123.45")
    }

    "fail to bind a value with only one decimal place" in {
      val result = form.bind(Map(fieldName -> "123.4"))
      result.errors must contain only FormError(fieldName, "paymentAmount.error.invalidNumeric")
    }

    "fail to bind a value with more than two decimal places" in {
      val result = form.bind(Map(fieldName -> "123.456"))
      result.errors must contain only FormError(fieldName, "paymentAmount.error.invalidNumeric")
    }

    "unbinds value with two decimal places for redisplay" in {
      val unbound = form.fill(BigDecimal("123.40")).apply(fieldName)
      unbound.value.value mustBe "123.40"
    }

    "unbinds whole number as .00 for redisplay" in {
      val unbound = form.fill(BigDecimal("50")).apply(fieldName)
      unbound.value.value mustBe "50.00"
    }
  }
}
