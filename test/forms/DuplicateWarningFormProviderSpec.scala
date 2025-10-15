package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class DuplicateWarningFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey = "duplicateWarning.error.required"
  private val invalidKey = "error.boolean"

  val form = new DuplicateWarningFormProvider()()

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
