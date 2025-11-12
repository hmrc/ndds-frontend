package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class AddPaymentPlanEndDateFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "addPaymentPlanEndDate.error.required"
  val invalidKey = "error.boolean"

  val form = new AddPaymentPlanEndDateFormProvider()()

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
