/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package viewmodels.govuk

import play.api.data.Field
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.FormGroup
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.dateinput.{DateInput, InputItem}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.{Fieldset, Legend}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import utils.DateFormats
import utils.Utils.emptyString
import viewmodels.ErrorMessageAwareness

object date extends DateFluency

trait DateFluency {

  object DateViewModel extends ErrorMessageAwareness {

    def apply(
      field: Field,
      legend: Legend
    )(implicit messages: Messages): DateInput =
      apply(
        field    = field,
        fieldset = Fieldset(legend = Some(legend))
      )

    def apply(
      field: Field,
      fieldset: Fieldset
    )(implicit messages: Messages): DateInput = {

      val errorClass = "govuk-input--error"

      // Updated error detection logic: highlight if any error on subfield
      val dayError = field("day").error.isDefined
      val monthError = field("month").error.isDefined
      val yearError = field("year").error.isDefined
      val anySpecificError = dayError || monthError || yearError
      val allFieldsError = field.error.isDefined && !anySpecificError

      val dayErrorClass = if (dayError || allFieldsError) errorClass else emptyString
      val monthErrorClass = if (monthError || allFieldsError) errorClass else emptyString
      val yearErrorClass = if (yearError || allFieldsError) errorClass else emptyString

      val items = Seq(
        InputItem(
          id      = s"${field.id}.day",
          name    = s"${field.name}.day",
          value   = field("day").value,
          label   = Some(messages("date.day")),
          classes = s"govuk-input--width-2 $dayErrorClass".trim
        ),
        InputItem(
          id      = s"${field.id}.month",
          name    = s"${field.name}.month",
          value   = field("month").value,
          label   = Some(messages("date.month")),
          classes = s"govuk-input--width-2 $monthErrorClass".trim
        ),
        InputItem(
          id      = s"${field.id}.year",
          name    = s"${field.name}.year",
          value   = field("year").value,
          label   = Some(messages("date.year")),
          classes = s"govuk-input--width-4 $yearErrorClass".trim
        )
      )

      DateInput(
        fieldset     = Some(fieldset),
        items        = items,
        id           = field.id,
        errorMessage = dateFieldErrorMessage(field)
      )
    }
  }

  object YearMonthViewModel extends ErrorMessageAwareness {
    def apply(
      field: Field,
      legend: Legend
    )(implicit messages: Messages): DateInput =
      apply(
        field    = field,
        fieldset = Fieldset(legend = Some(legend))
      )

    def apply(
      field: Field,
      fieldset: Fieldset
    )(implicit messages: Messages): DateInput = {
      val errorClass = "govuk-input--error"
      val monthError = field("month").error.isDefined
      val yearError = field("year").error.isDefined
      val anySpecificError = monthError || yearError
      val allFieldsError = field.error.isDefined && !anySpecificError
      val monthErrorClass = if (monthError || allFieldsError) errorClass else emptyString
      val yearErrorClass = if (yearError || allFieldsError) errorClass else emptyString
      val items = Seq(
        InputItem(
          id      = s"${field.id}.year",
          name    = s"${field.name}.year",
          value   = field("year").value,
          label   = Some(messages("date.year")),
          classes = s"govuk-input--width-4 $yearErrorClass".trim
        ),
        InputItem(
          id      = s"${field.id}.month",
          name    = s"${field.name}.month",
          value   = field("month").value,
          label   = Some(messages("date.month")),
          classes = s"govuk-input--width-2 $monthErrorClass".trim
        )
      )
      DateInput(
        fieldset     = Some(fieldset),
        items        = items,
        id           = field.id,
        errorMessage = dateFieldErrorMessage(field)
      )
    }
  }

  implicit class FluentDate(date: DateInput) {

    def withNamePrefix(prefix: String): DateInput =
      date.copy(namePrefix = Some(prefix))

    def withHint(hint: Hint): DateInput =
      date.copy(hint = Some(hint))

    def withFormGroup(formGroup: FormGroup): DateInput =
      date.copy(formGroup = formGroup)

    def withCssClass(newClass: String): DateInput =
      date.copy(classes = s"${date.classes} $newClass")

    def withAttribute(attribute: (String, String)): DateInput =
      date.copy(attributes = date.attributes + attribute)

    def asDateOfBirth(): DateInput =
      date.copy(items = date.items map { item =>
        val name = item.id.split('.').last
        item.copy(autocomplete = Some(s"bday-$name"))
      })
  }

  // New helper to provide specific error messages for date fields
  private def dateFieldErrorMessage(field: Field)(implicit messages: Messages): Option[ErrorMessage] = {
    val dayErrorKey = DateFormats.defaultDateFormats.find(_.dateType == "day").map(_.errorKey).getOrElse("date.error.day")
    val monthErrorKey = DateFormats.defaultDateFormats.find(_.dateType == "month").map(_.errorKey).getOrElse("date.error.month")
    val yearErrorKey = DateFormats.defaultDateFormats.find(_.dateType == "year").map(_.errorKey).getOrElse("date.error.year")

    val dayError = field("day").error.exists(_.message == dayErrorKey)
    val monthError = field("month").error.exists(_.message == monthErrorKey)
    val yearError = field("year").error.exists(_.message == yearErrorKey)

    val errorMsg: Option[String] =
      if (dayError) Some(messages(dayErrorKey))
      else if (monthError) Some(messages(monthErrorKey))
      else if (yearError) Some(messages(yearErrorKey))
      else field.error.map(e => messages(e.message, e.args*))

    errorMsg.map { msg =>
      ErrorMessage(
        content            = uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text(msg),
        visuallyHiddenText = Some(messages("error.prefix"))
      )
    }
  }
}
