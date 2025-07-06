package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.AuthorisedAccountHolder

class AuthorisedAccountHolderFormProvider @Inject() extends Mappings {

  def apply(): Form[AuthorisedAccountHolder] =
    Form(
      "value" -> enumerable[AuthorisedAccountHolder]("authorisedAccountHolder.error.required")
    )
}
