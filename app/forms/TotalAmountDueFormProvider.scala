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

import config.FrontendAppConfig
import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class TotalAmountDueFormProvider @Inject() (config: FrontendAppConfig) extends Mappings {

  private val maximum = 99999999.99
  private val minimum = config.minimumLiabilityAmount

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currencyWithTwoDecimalsOrWholeNumber(
        requiredKey       = "totalAmountDue.error.required",
        invalidNumericKey = "totalAmountDue.error.invalidNumeric",
        nonNumericKey     = "totalAmountDue.error.nonNumeric"
      ).verifying(
        maximumCurrency(maximum, "totalAmountDue.error.aboveMaximum"),
        minimumCurrency(minimum, "totalAmountDue.error.belowMinimum")
      )
    )

}
