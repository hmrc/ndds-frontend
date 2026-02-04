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
import models.DirectDebitSource.{MGD, SA, TC}
import models.{DirectDebitSource, PaymentPlanType}
import play.api.data.Form

import javax.inject.Inject

class PaymentPlanTypeFormProvider @Inject() extends Mappings {

  def apply(selectedAnswer: Option[DirectDebitSource]): Form[PaymentPlanType] = {

    val errorKey =
      selectedAnswer match {
        case Some(MGD) => "paymentPlanType.error.required.mgd"
        case Some(SA)  => "paymentPlanType.error.required.sa"
        case Some(TC)  => "paymentPlanType.error.required.tc"
        case _         => "paymentPlanType.error.required"
      }

    Form(
      "value" -> enumerable[PaymentPlanType](errorKey)
    )
  }
}
