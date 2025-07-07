package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class paymentReferenceFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("paymentReference.error.required")
        .verifying(maxLength(100, "paymentReference.error.length"))
    )
}
