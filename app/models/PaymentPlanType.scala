package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait PaymentPlanType

object PaymentPlanType extends Enumerable.Implicits {

  case object ASinglePayment extends WithName("a single payment") with PaymentPlanType
  case object AVariablePaymentPlan extends WithName("a variable payment plan") with PaymentPlanType

  val values: Seq[PaymentPlanType] = Seq(
    ASinglePayment, AVariablePaymentPlan
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"paymentPlanType.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[PaymentPlanType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
