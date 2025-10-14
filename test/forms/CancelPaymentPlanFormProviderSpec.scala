package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class CancelPaymentPlanFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "cancelPaymentPlan.error.required"
  val invalidKey = "error.boolean"

  val form = new CancelPaymentPlanFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
