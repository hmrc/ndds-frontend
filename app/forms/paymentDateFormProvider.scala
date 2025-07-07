package forms

import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class paymentDateFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "paymentDate.error.invalid",
        allRequiredKey = "paymentDate.error.required.all",
        twoRequiredKey = "paymentDate.error.required.two",
        requiredKey    = "paymentDate.error.required"
      )
    )
}
