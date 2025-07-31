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

import models.YearEndAndMonth
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.api.data.FormError

class YearEndAndMonthFormProviderSpec extends forms.behaviours.FieldBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form = new YearEndAndMonthFormProvider()()

  ".value" - {

    "bind valid data" in {
      val data = Map(
        "value.month" -> "05",
        "value.year"  -> "2024"
      )

      val result = form.bind(data)

      val expected = YearEndAndMonth(2024, 5)
      result.value.value mustEqual expected
      result.errors mustBe empty
    }

    "bind valid data with month 13" in {
      val data = Map(
        "value.month" -> "13",
        "value.year"  -> "2024"
      )

      val result = form.bind(data)

      val expected = YearEndAndMonth(2024, 13)
      result.value.value mustEqual expected
      result.errors mustBe empty
    }

    "fail to bind an empty date" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain theSameElementsAs List(
        FormError("value.month", List("date.error.month")),
        FormError("value.year", List("date.error.year"))
      )
    }
  }
}
