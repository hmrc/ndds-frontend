package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.PaymentPlanType

class PaymentPlanTypeFormProvider @Inject() extends Mappings {

  def apply(): Form[PaymentPlanType] =
    Form(
      "value" -> enumerable[PaymentPlanType]("paymentPlanType.error.required")
    )
}
