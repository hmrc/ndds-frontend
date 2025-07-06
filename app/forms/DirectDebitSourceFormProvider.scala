package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.DirectDebitSource

class DirectDebitSourceFormProvider @Inject() extends Mappings {

  def apply(): Form[DirectDebitSource] =
    Form(
      "value" -> enumerable[DirectDebitSource]("directDebitSource.error.required")
    )
}
