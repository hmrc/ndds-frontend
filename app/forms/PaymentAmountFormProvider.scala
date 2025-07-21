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

import forms.mappings.Mappings

import javax.inject.Inject
import play.api.data.Form
import utils.Constants._

class PaymentAmountFormProvider @Inject() extends Mappings {

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "paymentAmount.error.required",
        "paymentAmount.error.invalidNumeric",
        "paymentAmount.error.nonNumeric"
      )
        .transform[BigDecimal](
          _.setScale(2, BigDecimal.RoundingMode.HALF_UP),
          identity
        )
        .verifying("paymentAmount.error.moreThanTwoDecimals", amount => amount.scale <= DECIMAL_SCALE)
        .verifying(minimumValue[BigDecimal](MIN_AMOUNT, "paymentAmount.error.max.min.range"))
        .verifying(maximumValue[BigDecimal](MAX_AMOUNT, "paymentAmount.error.max.min.range"))
    )

}

