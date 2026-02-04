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

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class RegularPaymentAmountFormProvider @Inject() extends Mappings {
  val MIN_AMOUNT: BigDecimal = 1.0
  val MAX_AMOUNT: BigDecimal = 20000000.00

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currencyWithTwoDecimalsOrWholeNumber(
        requiredKey       = "regularPaymentAmount.error.required",
        invalidNumericKey = "regularPaymentAmount.error.invalidNumeric",
        nonNumericKey     = "regularPaymentAmount.error.nonNumeric"
      )
        .verifying(maximumCurrency(MAX_AMOUNT, "regularPaymentAmount.error.aboveMaximum"))
        .verifying(minimumCurrency(MIN_AMOUNT, "regularPaymentAmount.error.belowMinimum"))
    )
}
