package forms

import config.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class PaymentAmountFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new PaymentAmountFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 1.00
    val maximum = 2000000.00

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "paymentAmount.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "paymentAmount.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "paymentAmount.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "paymentAmount.error.required")
    )
  }
}
