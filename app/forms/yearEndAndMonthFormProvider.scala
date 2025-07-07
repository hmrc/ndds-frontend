package forms

import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class yearEndAndMonthFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "yearEndAndMonth.error.invalid",
        allRequiredKey = "yearEndAndMonth.error.required.all",
        twoRequiredKey = "yearEndAndMonth.error.required.two",
        requiredKey    = "yearEndAndMonth.error.required"
      )
    )
}
