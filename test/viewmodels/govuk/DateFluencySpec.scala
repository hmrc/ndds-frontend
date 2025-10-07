/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.mappings.Mappings
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import viewmodels.govuk.all.*

import java.time.LocalDate

class DateFluencySpec extends AnyFreeSpec with Matchers with Mappings with OptionValues {

  ".apply" - {

    implicit val messages: Messages = stubMessages()

    val fieldset = FieldsetViewModel(LegendViewModel("foo"))

    val form: Form[LocalDate] =
      Form(
        "value" -> localDate(
          invalidKey     = "fieldName.error.invalid",
          allRequiredKey = "fieldName.error.required.all",
          twoRequiredKey = "fieldName.error.required.two",
          requiredKey    = "fieldName.error.required"
        )
      )

    val errorClass = "govuk-input--error"

    "must highlight all fields and show the general error message when there is an error, but the error does not specify any individual day/month/year field" in {

      val boundForm = form.withError("value", "fieldName.error.required.all").bind(Map.empty[String, String])

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.forall(_.classes.contains(errorClass)) mustEqual true
      result.errorMessage.value.content.asHtml.toString must include(messages("fieldName.error.required.all"))
    }

    "must highlight the day field and show the day error message when the error is that a day is missing" in {

      val boundForm = form
        .withError("value.day", "date.error.day")
        .bind(
          Map(
            "value.month" -> "1",
            "value.year"  -> "2000"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must include(errorClass)
      result.items.find(_.id == "value.month").value.classes must not include errorClass
      result.items.find(_.id == "value.year").value.classes  must not include errorClass
      result.errorMessage.value.content.asHtml.toString      must include(messages("date.error.day"))
    }

    "must highlight the day and month fields and show the general error message when the error is that a day and month are both missing" in {

      val boundForm = form
        .withError("value.day", "date.error.day")
        .withError("value.month", "date.error.month")
        .bind(
          Map(
            "value.year" -> "2000"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must include(errorClass)
      result.items.find(_.id == "value.month").value.classes must include(errorClass)
      result.items.find(_.id == "value.year").value.classes  must not include errorClass
      // Should show the day error message (first in logic)
      result.errorMessage.value.content.asHtml.toString must include(messages("date.error.day"))
    }

    "must highlight the day and year fields and show the day error message when the error is that a day and year are both missing" in {

      val boundForm = form
        .withError("value.day", "date.error.day")
        .withError("value.year", "date.error.year")
        .bind(
          Map(
            "value.month" -> "1"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must include(errorClass)
      result.items.find(_.id == "value.month").value.classes must not include errorClass
      result.items.find(_.id == "value.year").value.classes  must include(errorClass)
      result.errorMessage.value.content.asHtml.toString      must include(messages("date.error.day"))
    }

    "must highlight the month field and show the month error message when the error is that a month is missing" in {

      val boundForm = form
        .withError("value.month", "date.error.month")
        .bind(
          Map(
            "value.day"  -> "1",
            "value.year" -> "2000"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must not include errorClass
      result.items.find(_.id == "value.month").value.classes must include(errorClass)
      result.items.find(_.id == "value.year").value.classes  must not include errorClass
      result.errorMessage.value.content.asHtml.toString      must include(messages("date.error.month"))
    }

    "must highlight the month and year fields and show the month error message when the error is that a month and year are both missing" in {

      val boundForm = form
        .withError("value.month", "date.error.month")
        .withError("value.year", "date.error.year")
        .bind(
          Map(
            "value.day" -> "1"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must not include errorClass
      result.items.find(_.id == "value.month").value.classes must include(errorClass)
      result.items.find(_.id == "value.year").value.classes  must include(errorClass)
      result.errorMessage.value.content.asHtml.toString      must include(messages("date.error.month"))
    }

    "must highlight the year field and show the year error message when the error is that a year is missing" in {

      val boundForm = form
        .withError("value.year", "date.error.year")
        .bind(
          Map(
            "value.day"   -> "1",
            "value.month" -> "1"
          )
        )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.find(_.id == "value.day").value.classes   must not include errorClass
      result.items.find(_.id == "value.month").value.classes must not include errorClass
      result.items.find(_.id == "value.year").value.classes  must include(errorClass)
      result.errorMessage.value.content.asHtml.toString      must include(messages("date.error.year"))
    }

    "must not highlight any fields and have no error message when there is not an error" in {

      val boundForm = form.bind(
        Map(
          "value.day"   -> "1",
          "value.month" -> "1",
          "value.year"  -> "2000"
        )
      )

      val result = DateViewModel(boundForm("value"), fieldset)

      result.items.forall(_.classes.contains(errorClass)) mustEqual false
      result.errorMessage mustBe None
    }
  }
}
