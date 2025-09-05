package forms

import models.ConfirmAuthority
import play.api.data.FormError
import forms.behaviours.OptionFieldBehaviours

class ConfirmAuthorityFormProviderSpec extends OptionFieldBehaviours {
  val form = new ConfirmAuthorityFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "confirmYourAuthority.error.required"

    behave like optionsField[ConfirmAuthority](
      form,
      fieldName,
      validValues  = ConfirmAuthority.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
