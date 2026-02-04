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

import javax.inject.Inject
import models.DirectDebitSource
import forms.mappings.Mappings
import play.api.data.Form

class PaymentReferenceFormProvider @Inject() extends Mappings {

  private def invalidKeyFor(source: Option[DirectDebitSource]): String =
    source match {
      case Some(DirectDebitSource.CT)   => "paymentReference.error.invalid.ct"
      case Some(DirectDebitSource.MGD)  => "paymentReference.error.invalid.mgd"
      case Some(DirectDebitSource.NIC)  => "paymentReference.error.invalid.nic"
      case Some(DirectDebitSource.PAYE) => "paymentReference.error.invalid.paye"
      case Some(DirectDebitSource.SA)   => "paymentReference.error.invalid.sa"
      case Some(DirectDebitSource.SDLT) => "paymentReference.error.invalid.sdlt"
      case Some(DirectDebitSource.TC)   => "paymentReference.error.invalid.tc"
      case Some(DirectDebitSource.VAT)  => "paymentReference.error.invalid.vat"
      case Some(DirectDebitSource.OL)   => "paymentReference.error.invalid.other"
      case _                            => "paymentReference.error.invalid"
    }

  def apply(selectedSource: Option[DirectDebitSource], validator: Option[String => Boolean] = None): Form[String] = {
    val invalidKey = invalidKeyFor(selectedSource)

    Form(
      "value" -> text(invalidKey)
        .verifying(invalidKey, reference => validator.forall(_(reference)))
    )
  }
}
