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

import config.FrontendAppConfig
import config.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class TotalAmountDueFormProviderSpec extends CurrencyFieldBehaviours with MockitoSugar {

  private val fieldName = "value"
  private val min = BigDecimal(12.00)
  private val max = BigDecimal(99999999.99)

  private def formWithMin(minValue: BigDecimal) = {
    val config = mock[FrontendAppConfig]
    when(config.minimumLiabilityAmount).thenReturn(minValue)
    new TotalAmountDueFormProvider(config)()
  }

  private val form = formWithMin(min)

  private val validDataGenerator: Gen[String] =
    Gen
      .choose[BigDecimal](min, max)
      .map(_.setScale(2, RoundingMode.HALF_UP))
      .map(_.toString)

  ".value" - {

    behave like fieldThatBindsValidData(form, fieldName, validDataGenerator)

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "totalAmountDue.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "totalAmountDue.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      max,
      FormError(fieldName, "totalAmountDue.error.aboveMaximum", Seq(currencyFormat(max)))
    )

    behave like currencyFieldWithMinimum(
      form,
      fieldName,
      min,
      FormError(fieldName, "totalAmountDue.error.belowMinimum", Seq(currencyFormat(min)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "totalAmountDue.error.required")
    )

    "fail when below minimum" in {
      val result = form.bind(Map(fieldName -> (min - 0.01).toString))
      result.errors must contain only FormError(fieldName, "totalAmountDue.error.belowMinimum", Seq(currencyFormat(min)))
    }

    "bind a whole number and format it as .00" in {
      val result = form.bind(Map(fieldName -> "1000"))
      result.errors mustBe empty
      result.value.value mustBe BigDecimal("1000.00")
    }

    "bind a value with exactly two decimal places" in {
      val result = form.bind(Map(fieldName -> "123.45"))
      result.errors mustBe empty
      result.value.value mustBe BigDecimal("123.45")
    }

    "fail to bind a value with only one decimal place" in {
      val result = form.bind(Map(fieldName -> "123.4"))
      result.errors must contain only FormError(fieldName, "totalAmountDue.error.invalidNumeric")
    }

    "fail to bind a value with more than two decimal places" in {
      val result = form.bind(Map(fieldName -> "123.456"))
      result.errors must contain only FormError(fieldName, "totalAmountDue.error.invalidNumeric")
    }

    "fail to bind negative values" in {
      val result = form.bind(Map(fieldName -> "-10.00"))
      result.errors must contain only FormError(fieldName, "totalAmountDue.error.belowMinimum", Seq(currencyFormat(min)))
    }

    "unbinds value with two decimal places for redisplay" in {
      val unbound = form.fill(BigDecimal("123.40")).apply(fieldName)
      unbound.value.value mustBe "123.40"
    }

    "unbinds whole number as .00 for redisplay" in {
      val unbound = form.fill(BigDecimal("50")).apply(fieldName)
      unbound.value.value mustBe "50.00"
    }

    "must validate dynamic minimum liability from config" in {
      val testForm = formWithMin(BigDecimal("25.00"))
      val result = testForm.bind(Map(fieldName -> "10.00"))
      result.errors must contain only FormError(fieldName, "totalAmountDue.error.belowMinimum", Seq("£25"))
    }

    "must accept the configured minimum value (inclusive)" in {
      val testForm = formWithMin(BigDecimal("25.00"))
      val result = testForm.bind(Map(fieldName -> "25.00"))
      result.errors mustBe empty
      result.value.value mustBe BigDecimal("25.00")
    }
  }
}
