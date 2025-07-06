package forms

import forms.behaviours.OptionFieldBehaviours
import models.AuthorisedAccountHolder
import play.api.data.FormError

class AuthorisedAccountHolderFormProviderSpec extends OptionFieldBehaviours {

  val form = new AuthorisedAccountHolderFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "authorisedAccountHolder.error.required"

    behave like optionsField[AuthorisedAccountHolder](
      form,
      fieldName,
      validValues  = AuthorisedAccountHolder.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
