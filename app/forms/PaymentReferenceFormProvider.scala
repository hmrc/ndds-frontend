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

  private def requiredKey(source: Option[DirectDebitSource]) =
    source
      .map(s => s"paymentReference.error.required.${s.toString.toLowerCase}")
      .getOrElse("paymentReference.error.required")

  private def lengthKey(source: Option[DirectDebitSource]) =
    source
      .map(s => s"paymentReference.error.length.${s.toString.toLowerCase}")
      .getOrElse("paymentReference.error.invalid")

  private def invalidKey(source: Option[DirectDebitSource]) =
    source
      .map(s => s"paymentReference.error.invalid.${s.toString.toLowerCase}")
      .getOrElse("paymentReference.error.invalid")

  private def expectedLength(source: DirectDebitSource): String => Boolean = {
    case ref if source == DirectDebitSource.CT   => ref.length == 17
    case ref if source == DirectDebitSource.MGD  => ref.length == 14
    case ref if source == DirectDebitSource.NIC  => ref.length == 18
    case ref if source == DirectDebitSource.PAYE => ref.length == 13 || ref.length == 14
    case ref if source == DirectDebitSource.SA   => ref.length == 10
    case ref if source == DirectDebitSource.SDLT => ref.length == 11
    case ref if source == DirectDebitSource.TC   => ref.length == 16
    case ref if source == DirectDebitSource.VAT  => ref.length == 9
    case _                                       => true
  }

  def apply(
    source: Option[DirectDebitSource],
    validator: Option[String => Boolean]
  ): Form[String] =
    Form(
      "value" -> text(requiredKey(source))
        .transform[String](_.trim, identity)
        .verifying(
          lengthKey(source),
          ref => source.forall(expectedLength(_)(ref))
        )
        .verifying(
          invalidKey(source),
          ref =>
            source match {
              case Some(s) if !expectedLength(s)(ref) =>
                true
              case _ =>
                validator.forall(_(ref))
            }
        )
    )
}
