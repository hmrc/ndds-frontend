package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait DirectDebitSource

object DirectDebitSource extends Enumerable.Implicits {

  case object CorporationTax extends WithName("corporation Tax") with DirectDebitSource
  case object MachineGamesDuty extends WithName("machine Games Duty") with DirectDebitSource

  val values: Seq[DirectDebitSource] = Seq(
    CorporationTax, MachineGamesDuty
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"directDebitSource.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[DirectDebitSource] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
