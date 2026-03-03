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

  private val validCharactersRegex = "^[A-Za-z0-9]+$".r

  private def key(source: DirectDebitSource, suffix: String) =
    s"paymentReference.${source.toString}.$suffix"

  def apply(
    source: Option[DirectDebitSource],
    validator: Option[String => Boolean]
  ): Form[String] = {

    val src = source.getOrElse(
      throw new IllegalStateException("DirectDebitSource must be defined")
    )

    Form(
      "value" -> text(key(src, "required"))
        .transform[String](_.trim.toUpperCase, identity)
        .verifying(key(src, "required"), _.nonEmpty)
        .verifying(
          key(src, "invalidCharacters"),
          value => value.isEmpty || validCharactersRegex.matches(value)
        )
        .verifying(
          key(src, "invalidFormat"),
          value =>
            value.isEmpty ||
              !validCharactersRegex.matches(value) ||
              formatCheck(src, value)
        )
        .verifying(
          key(src, "invalid"),
          value =>
            value.isEmpty ||
              !validCharactersRegex.matches(value) ||
              !formatCheck(src, value) ||
              validator.forall(_(value))
        )
    )
  }

  // Only basic structural checks here (length/regex),
  // NOT modulus checks.
  private def formatCheck(source: DirectDebitSource, ref: String): Boolean =
    source match {
      case DirectDebitSource.CT   => ref.length == 17
      case DirectDebitSource.MGD  => ref.length == 14
      case DirectDebitSource.NIC  => ref.length == 18
      case DirectDebitSource.PAYE => ref.length == 13 || ref.length == 14
      case DirectDebitSource.SA   => ref.length == 10 || ref.length == 11
      case DirectDebitSource.SDLT => ref.length == 11
      case DirectDebitSource.TC   => ref.length == 16
      case DirectDebitSource.VAT  => ref.length == 9
      case DirectDebitSource.OL   => ref.length >= 14 && ref.length <= 15
    }
}
