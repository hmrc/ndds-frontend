package forms

import forms.behaviours.{BooleanFieldBehaviours, OptionFieldBehaviours}
import models.DuplicateWarning
import play.api.data.FormError

class DuplicateWarningFormProviderSpec extends OptionFieldBehaviours {

  val form = new DuplicateWarningFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "duplicateWarning.error.required"

    behave like optionsField[DuplicateWarning](
      form,
      fieldName,
      validValues  = DuplicateWarning.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
