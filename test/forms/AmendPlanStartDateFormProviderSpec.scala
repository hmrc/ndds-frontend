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

import java.time.{LocalDate, ZoneOffset}
import forms.behaviours.DateBehaviours
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class AmendPlanStartDateFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()
  private val startDate = LocalDate.of(2024, 4, 6)
  private val form = new AmendPlanStartDateFormProvider()()

  "PlanEndDateFormProvider" - {

    "must bind valid dates after or equal to the plan start date" in {
      val validDate = startDate
      val result = form.bind(
        Map(
          "value.day" -> validDate.getDayOfMonth.toString,
          "value.month" -> validDate.getMonthValue.toString,
          "value.year" -> validDate.getYear.toString
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
          FormError("value.day", "date.error.day"),
          FormError("value.month", "date.error.month"),
          FormError("value.year", "date.error.year")
        )
      }
    }

  }
}
