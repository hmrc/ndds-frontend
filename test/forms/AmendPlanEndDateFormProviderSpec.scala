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

import java.time.{LocalDate, ZoneOffset}

class AmendPlanEndDateFormProviderSpec extends DateBehaviours {
  private implicit val messages: Messages = stubMessages()
  private val endDate = LocalDate.of(2024, 4, 6)
  private val form = new AmendPlanEndDateFormProvider()()

  "AmendPlanEndDateFormProvider" - {

    "must bind valid dates after or equal to the plan start date" in {
      val validDate = endDate
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

      "bind valid data" in {
        forAll(validData -> "valid date") { (date: LocalDate) =>
          val data = Map(
            "value.day"   -> date.getDayOfMonth.toString,
            "value.month" -> date.getMonthValue.toString,
            "value.year"  -> date.getYear.toString
          )

          val result = form.bind(data)
          result.value.flatten mustBe Some(date)
          result.errors mustBe empty
        }
      }

      "fail to bind an empty date" in {
        val result = form.bind(Map.empty[String, String])
        result.errors mustBe empty
        result.value mustBe Some(None)
      }
    }
  }
}
