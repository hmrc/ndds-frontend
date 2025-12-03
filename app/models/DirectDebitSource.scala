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

package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait DirectDebitSource

object DirectDebitSource extends Enumerable.Implicits {

  case object CT extends WithName("ct") with DirectDebitSource

  case object MGD extends WithName("mgd") with DirectDebitSource

  case object NIC extends WithName("nic") with DirectDebitSource

  case object OL extends WithName("otherLiability") with DirectDebitSource

  case object PAYE extends WithName("paye") with DirectDebitSource

  case object SA extends WithName("sa") with DirectDebitSource

  case object SDLT extends WithName("sdlt") with DirectDebitSource

  case object TC extends WithName("tc") with DirectDebitSource

  case object VAT extends WithName("vat") with DirectDebitSource

  val values: Seq[DirectDebitSource] = Seq(
    CT,
    MGD,
    NIC,
    PAYE,
    SA,
    SDLT,
    TC,
    VAT,
    OL
  )

  val hodServiceMapping: Map[String, String] = Map(
    "COTA" -> CT.toString,
    "NIDN" -> NIC.toString,
    "SAFE" -> OL.toString,
    "PAYE" -> PAYE.toString,
    "CESA" -> SA.toString,
    "SDLT" -> SDLT.toString,
    "NTC"  -> TC.toString,
    "VAT"  -> VAT.toString,
    "MGD"  -> MGD.toString
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    val hintKey = s"directDebitSource.${value.toString}.hint"
    val hintText = messages(hintKey)
    RadioItem(
      content = Text(messages(s"directDebitSource.${value.toString}")),
      value   = Some(value.toString),
      id      = Some(s"value_$index"),
      hint    = if (hintText != hintKey) Some(Hint(content = Text(hintText))) else None
    )
  }

  implicit val enumerable: Enumerable[DirectDebitSource] =
    Enumerable(values.map(v => v.toString -> v)*)

  val objectMap: Map[String, DirectDebitSource] = values.map(v => v.toString -> v).toMap
}
