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

package forms

import forms.behaviours.DateBehaviours
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class AmendPlanEndDateFormProviderSpec extends DateBehaviours {

  implicit private val messages: Messages = stubMessages()

  private val form = new AmendPlanEndDateFormProvider()()
  private val validDate = LocalDate.of(2024, 4, 6)

  "AmendPlanEndDateFormProvider" - {

    "must bind a valid date" in {
      val result = form.bind(
        Map(
          "value.day" -> validDate.getDayOfMonth.toString,
          "value.month" -> validDate.getMonthValue.toString,
          "value.year" -> validDate.getYear.toString
        )
      )
      result.errors mustBe empty
//      result.value must contain(Some(validDate))
    }

    "must return error for partially completed date" in {
      val result = form.bind(
        Map(
          "value.day" -> "",
          "value.month" -> "4",
          "value.year" -> ""
        )
      )

      result.errors must contain only FormError(
        key = "value",
        message = "planEndDate.error.required.two",
        args = Seq("date.error.day", "date.error.year")
      )
    }

    "must return error when all fields are empty" in {
      val result = form.bind(Map("value.day" -> "", "value.month" -> "", "value.year" -> ""))

      result.errors must contain only FormError("value", "planEndDate.error.required.all")
    }

    "must return error when date is invalid (e.g. 31st April)" in {
      val result = form.bind(
        Map(
          "value.day" -> "31",
          "value.month" -> "4",
          "value.year" -> "2024"
        )
      )

      result.errors must contain only FormError("value", "planEndDate.error.invalid")
    }

    "must return error when non-numeric values are provided" in {
      val result = form.bind(
        Map(
          "value.day" -> "ab",
          "value.month" -> "cd",
          "value.year" -> "efgh"
        )
      )

      result.errors must contain only FormError("value", "planEndDate.error.invalid")
    }
  }
}
