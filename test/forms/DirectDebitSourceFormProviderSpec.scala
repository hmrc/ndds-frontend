package forms

import forms.behaviours.OptionFieldBehaviours
import models.DirectDebitSource
import play.api.data.FormError

class DirectDebitSourceFormProviderSpec extends OptionFieldBehaviours {

  val form = new DirectDebitSourceFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "directDebitSource.error.required"

    behave like optionsField[DirectDebitSource](
      form,
      fieldName,
      validValues  = DirectDebitSource.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
