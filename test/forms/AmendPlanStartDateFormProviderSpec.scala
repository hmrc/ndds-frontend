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

import java.time.{Clock, LocalDate, ZoneId, ZoneOffset}

class AmendPlanStartDateFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()

  private val fixedDate = LocalDate.of(2025, 8, 6)
  private val fixedClock = Clock.fixed(
    fixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant,
    ZoneId.systemDefault()
  )

  private val startDate = LocalDate.of(2024, 4, 6)
  private val form = new AmendPlanStartDateFormProvider(fixedClock)()

  "AmendPlanStartDateFormProvider" - {

    "must bind valid dates" in {
      val validDate = startDate
      val result = form.bind(
        Map(
          "value.day"   -> validDate.getDayOfMonth.toString,
          "value.month" -> validDate.getMonthValue.toString,
          "value.year"  -> validDate.getYear.toString
        )
      )
      result.errors mustBe empty
    }

    ".value" - {
      val validData = datesBetween(
        min = LocalDate.of(2000, 1, 1),
        max = LocalDate.now(ZoneOffset.UTC)
      )

      behave like dateField(form, "value", validData)

      "fail to bind an empty date" in {
        val result = form.bind(Map.empty[String, String])

        result.errors must contain theSameElementsAs Seq(
          FormError("value", "planStartDate.error.required.all")
        )
      }

      "fail to bind when day is missing" in {
        val result = form.bind(
          Map(
            "value.day"   -> "",
            "value.month" -> "8",
            "value.year"  -> "2025"
          )
        )

        result.errors must contain theSameElementsAs Seq(
          FormError("value.day", "planStartDate.error.required")
        )
      }

      "fail to bind when month is missing" in {
        val result = form.bind(
          Map(
            "value.day"   -> "6",
            "value.month" -> "",
            "value.year"  -> "2025"
          )
        )

        result.errors must contain theSameElementsAs Seq(
          FormError("value.month", "planStartDate.error.required")
        )
      }

      "fail to bind when year is missing" in {
        val result = form.bind(
          Map(
            "value.day"   -> "6",
            "value.month" -> "8",
            "value.year"  -> ""
          )
        )

        result.errors must contain theSameElementsAs Seq(
          FormError("value.year", "planStartDate.error.required")
        )
      }
    }
  }
}
