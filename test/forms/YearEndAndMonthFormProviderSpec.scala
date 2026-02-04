/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class YearEndAndMonthFormProviderSpec extends forms.behaviours.FieldBehaviours {

  implicit private val messages: Messages = stubMessages()
  private val form = new YearEndAndMonthFormProvider()()

  ".value" - {

    "bind valid data (e.g. 2505)" in {
      val data = Map(
        "value" -> "2505"
      )

      val result = form.bind(data)

      result.value.value mustEqual YearEndAndMonth(25, 5)
      result.errors mustBe empty
    }

    "bind valid data with month 13 (e.g. 2513)" in {
      val data = Map(
        "value" -> "2513"
      )

      val result = form.bind(data)

      result.value.value mustEqual YearEndAndMonth(25, 13)
      result.errors mustBe empty
    }

    "fail when field is empty" in {
      val data = Map("value" -> "")

      val result = form.bind(data)

      result.errors mustEqual Seq(
        FormError("value", "yearEndAndMonth.error.empty")
      )
    }

    "fail when month is outside 01-13" in {
      val data = Map("value" -> "2514")

      val result = form.bind(data)

      result.errors mustEqual Seq(
        FormError("value", "yearEndAndMonth.error.invalidMonth")
      )
    }

    "fail when not 4 digits" in {
      val data = Map("value" -> "25")

      val result = form.bind(data)

      result.errors mustEqual Seq(
        FormError("value", "yearEndAndMonth.error.invalid")
      )
    }

  }
}
