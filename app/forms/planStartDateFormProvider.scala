package forms

import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class planStartDateFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "planStartDate.error.invalid",
        allRequiredKey = "planStartDate.error.required.all",
        twoRequiredKey = "planStartDate.error.required.two",
        requiredKey    = "planStartDate.error.required"
      )
    )
}
