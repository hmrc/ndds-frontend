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
import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class TotalAmountDueFormProvider @Inject()(config: FrontendAppConfig) extends Mappings {
  val maximum = 999999
  val minimum = config.minimumLiabilityAmount

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "totalAmountDue.error.required",
        "totalAmountDue.error.invalidNumeric",
        "totalAmountDue.error.nonNumeric"
      )
      .verifying(maximumCurrency(maximum, "totalAmountDue.error.aboveMaximum"))
      .verifying(minimumCurrency(minimum, "totalAmountDue.error.belowMinimum"))
    )
}
