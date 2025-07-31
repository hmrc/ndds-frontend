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

import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class PaymentAmountFormProviderSpec extends CurrencyFieldBehaviours {

  val provider = new PaymentAmountFormProvider()
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
      nonNumericError     = FormError(fieldName, "paymentAmount.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "paymentAmount.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      max,
      FormError(fieldName, "paymentAmount.error.max.min.range", Seq(max))
    )

    "fail when below minimum" in {
      val result = form.bind(Map(fieldName -> (min - 1).toString))
      result.errors must contain only FormError(fieldName, "paymentAmount.error.max.min.range", Seq(min))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "paymentAmount.error.required")
    )
  }
}
