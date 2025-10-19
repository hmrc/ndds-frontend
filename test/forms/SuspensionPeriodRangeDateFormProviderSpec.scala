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
import models.SuspensionPeriodRange
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import java.time.LocalDate

class SuspensionPeriodRangeDateFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val form = new SuspensionPeriodRangeDateFormProvider()()

  private val validStartDate = LocalDate.of(2025, 10, 1)
  private val validEndDate = LocalDate.of(2025, 10, 10)

  "SuspensionPeriodRangeDateFormProvider" - {

    "must bind valid data correctly" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> validEndDate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> validEndDate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> validEndDate.getYear.toString
      )

      val result = form.bind(data)
      result.errors mustBe empty
      result.value.value mustBe SuspensionPeriodRange(validStartDate, validEndDate)
    }

    "must return an error when end date is before start date" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "10",
        "suspensionPeriodRangeStartDate.month" -> "10",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "05",
        "suspensionPeriodRangeEndDate.month"   -> "10",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endBeforeStart")
    }

    "must return generic date errors when start date is missing" in {
      val data = Map(
        "suspensionPeriodRangeEndDate.day"   -> "10",
        "suspensionPeriodRangeEndDate.month" -> "10",
        "suspensionPeriodRangeEndDate.year"  -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain allElementsOf Seq(
        "date.error.day",
        "date.error.month",
        "date.error.year"
      )
    }

    "must return generic date errors when end date is missing" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "10",
        "suspensionPeriodRangeStartDate.month" -> "10",
        "suspensionPeriodRangeStartDate.year"  -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain allElementsOf Seq(
        "date.error.day",
        "date.error.month",
        "date.error.year"
      )
    }

    "must return generic date errors when start date is invalid" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "32",
        "suspensionPeriodRangeStartDate.month" -> "13",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "10",
        "suspensionPeriodRangeEndDate.month"   -> "10",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain allElementsOf Seq(
        "date.error.day",
        "date.error.month"
      )
    }

    "must return generic date errors when end date is invalid" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "01",
        "suspensionPeriodRangeStartDate.month" -> "10",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "99",
        "suspensionPeriodRangeEndDate.month"   -> "99",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain allElementsOf Seq(
        "date.error.day",
        "date.error.month"
      )
    }
  }
}
