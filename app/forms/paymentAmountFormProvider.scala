package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class paymentAmountFormProvider @Inject() extends Mappings {

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "paymentAmount.error.required",
        "paymentAmount.error.invalidNumeric",
        "paymentAmount.error.nonNumeric"
      )
      .verifying(maximumCurrency(Int.MaxValue, "paymentAmount.error.aboveMaximum"))
    )
}
