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

  private val formatRegexMap: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT   -> "^\\d{10}A001\\d{2}A$",
    DirectDebitSource.MGD  -> "^X[A-Z]M0000\\d{7}$",
    DirectDebitSource.NIC  -> "^60\\d{15}[0-9X]$",
    DirectDebitSource.PAYE -> "^[A-Z0-9]{13,14}$",
    DirectDebitSource.SA   -> "^\\d{10}K$",
    DirectDebitSource.SDLT -> "^\\d{9}M[A-Z]$",
    DirectDebitSource.TC   -> "^[A-Z]{2}\\d{12}N[A-Z]$",
    DirectDebitSource.VAT  -> "^\\d{9,}$",
    DirectDebitSource.OL   -> "^(X[A-Z][A-Z0-9]\\d{11}|X[A-Z]ECL\\d{10}|X[A-Z]\\d{13})$"
  )

  private def formatCheck(source: DirectDebitSource, ref: String): Boolean =
    source match {
      case DirectDebitSource.PAYE => "^\\d{3}P[A-Z]\\d{7}(\\d|X)?$".r.matches(ref)

      case DirectDebitSource.OL =>
        val matches = formatRegexMap(DirectDebitSource.OL).r.matches(ref)
        matches && !(ref.length == 14 && ref.charAt(2) == 'M')

      case _ =>
        formatRegexMap.get(source).forall(regex => ref.matches(regex))
    }

  def apply(
    source: Option[DirectDebitSource],
    validator: Option[String => Boolean]
  ): Form[String] = {

    val src = source.getOrElse(
      throw new IllegalStateException("DirectDebitSource must be defined")
    )

    Form(
      "value" -> text(key(src, "required"))
        .transform[String](_.trim.toUpperCase.replaceAll(" ", ""), identity)
        .verifying(key(src, "required"), _.nonEmpty)
        .verifying(
          key(src, "invalidCharacters"),
          value => value.isEmpty || validCharactersRegex.matches(value)
        )
        .verifying(
          key(src, "invalidFormat"),
          value => value.isEmpty || !validCharactersRegex.matches(value) || formatCheck(src, value)
        )
        .verifying(
          key(src, "invalid"),
          value => value.isEmpty || !formatCheck(src, value) || validator.forall(_(value))
        )
    )
  }
}
