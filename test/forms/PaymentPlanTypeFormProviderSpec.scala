package forms

import forms.behaviours.OptionFieldBehaviours
import models.PaymentPlanType
import play.api.data.FormError

class PaymentPlanTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new PaymentPlanTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "paymentPlanType.error.required"

    behave like optionsField[PaymentPlanType](
      form,
      fieldName,
      validValues  = PaymentPlanType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
