package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait AuthorisedAccountHolder

object AuthorisedAccountHolder extends Enumerable.Implicits {

  case object Yes extends WithName("yes") with AuthorisedAccountHolder
  case object No extends WithName("no") with AuthorisedAccountHolder

  val values: Seq[AuthorisedAccountHolder] = Seq(
    Yes, No
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"authorisedAccountHolder.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[AuthorisedAccountHolder] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
