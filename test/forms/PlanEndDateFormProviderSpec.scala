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

import java.time.LocalDate
import forms.behaviours.DateBehaviours
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class PlanEndDateFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()
  private val startDate = LocalDate.of(2024, 4, 6)
  private val form = new PlanEndDateFormProvider()(startDate)

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
      result.value.flatten mustEqual Some(validDate)
    }

    "must fail to bind dates before the plan start date" in {
      val invalidDate = startDate.minusDays(1)
      val result = form.bind(
        Map(
          "value.day" -> invalidDate.getDayOfMonth.toString,
          "value.month" -> invalidDate.getMonthValue.toString,
          "value.year" -> invalidDate.getYear.toString
        )
      )
      result.errors must contain(FormError("value", "planEndDate.error.beforeOrEqualStartDate"))
    }

    "must bind successfully when date is left blank (optional)" in {
      val result = form.bind(Map.empty[String, String])
      result.errors mustBe empty
      result.value.flatten mustBe None
    }

    "must fail with required error if partially completed" in {
      val result = form.bind(
        Map(
          "value.day" -> "",
          "value.month" -> "4",
          "value.year" -> ""
        )
      )
      result.errors must contain(FormError("value", "planEndDate.error.incomplete", Seq("date.error.day", "date.error.year")))
    }
  }
}
