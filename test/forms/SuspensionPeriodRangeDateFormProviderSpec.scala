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

  private val planStartDate = LocalDate.of(2025, 10, 1)
  private val planEndDate = LocalDate.of(2025, 12, 31)
  private val earliestStartDate = LocalDate.of(2025, 9, 28) // 3 working days from today
  private val latestStartDate = LocalDate.now().plusMonths(6)

  private val formProvider = new SuspensionPeriodRangeDateFormProvider()
  private def form = formProvider(Some(planStartDate), Some(planEndDate), earliestStartDate)

  private val validStartDate = LocalDate.of(2025, 10, 10)
  private val validEndDate = LocalDate.of(2025, 10, 20)

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

    "must return an error when start date is before plan start" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "30",
        "suspensionPeriodRangeStartDate.month" -> "09",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "10",
        "suspensionPeriodRangeEndDate.month"   -> "10",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startBeforePlanStart")
    }

    "must return an error when start date is after plan end" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "01",
        "suspensionPeriodRangeStartDate.month" -> "01",
        "suspensionPeriodRangeStartDate.year"  -> "2026",
        "suspensionPeriodRangeEndDate.day"     -> "02",
        "suspensionPeriodRangeEndDate.month"   -> "01",
        "suspensionPeriodRangeEndDate.year"    -> "2026"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startAfterPlanEnd")
    }

    "must return an error when start date is before earliest allowed" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "25",
        "suspensionPeriodRangeStartDate.month" -> "09",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "30",
        "suspensionPeriodRangeEndDate.month"   -> "09",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startBeforeEarliestAllowed")
    }

    "must return an error when start date is after latest allowed" in {
      val tooLate = LocalDate.now().plusMonths(7)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> tooLate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> tooLate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> tooLate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> tooLate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> tooLate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> tooLate.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.startAfterLatestAllowed")
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

    "must return an error when end date is after 6 months from start date" in {
      val start = LocalDate.of(2025, 10, 1)
      val end = start.plusMonths(7)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> start.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> start.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> start.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> end.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> end.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> end.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endAfterLatestAllowed")
    }

    "must return an error when end date is after plan end date" in {
      val start = LocalDate.of(2025, 12, 30)
      val end = LocalDate.of(2026, 1, 5)
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> start.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> start.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> start.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> end.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> end.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> end.getYear.toString
      )

      val result = form.bind(data)
      result.errors.map(_.message) must contain("suspensionPeriodRangeDate.error.endAfterPlanEnd")
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
