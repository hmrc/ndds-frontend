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

import forms.behaviours.OptionFieldBehaviours
import models.PaymentPlanType
import play.api.data.FormError

class PaymentPlanTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new PaymentPlanTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "paymentPlanType.error.required"

    behave like optionsField[PaymentPlanType](
      form,
      fieldName,
      validValues  = PaymentPlanType.values2,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
