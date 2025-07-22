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

import config.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class RegularPaymentAmountFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new RegularPaymentAmountFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 1
    val maximum = 20000000

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "regularPaymentAmount.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "regularPaymentAmount.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "regularPaymentAmount.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like currencyFieldWithMinimum(
      form,
      fieldName,
      minimum,
      FormError(fieldName, "regularPaymentAmount.error.belowMinimum", Seq(currencyFormat(minimum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "regularPaymentAmount.error.required")
    )
  }
}
