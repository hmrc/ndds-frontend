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

  private val today = LocalDate.of(2025, 10, 1)
  private val earliestStart = today.plusDays(3)
  private val planStart = today
  private val planEnd = today.plusMonths(12)

  private val formProvider = new SuspensionPeriodRangeDateFormProvider()
  private val form = formProvider(Some(planStart), Some(planEnd), earliestStart)

  private val validStartDate = earliestStart.plusDays(1)
  private val validEndDate = validStartDate.plusDays(5)

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

    "must fail when start date is before earliest allowed date" in {
      val invalidStart = earliestStart.minusDays(1)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> invalidStart.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> invalidStart.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> invalidStart.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> validEndDate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> validEndDate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> validEndDate.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startDate")
    }

    "must fail when start date is after plan end date" in {
      val invalidStart = planEnd.plusDays(1)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> invalidStart.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> invalidStart.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> invalidStart.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> validEndDate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> validEndDate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> validEndDate.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startDate")
    }

    "must fail when end date is before start date" in {
      val invalidEnd = validStartDate.minusDays(1)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endDate")
    }

    "must fail when end date is after plan end date" in {
      val invalidEnd = planEnd.plusDays(1)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endDate")
    }

    "must fail when end date is more than 6 months after start date" in {
      val invalidEnd = validStartDate.plusMonths(7)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endDate")
    }
  }
}
