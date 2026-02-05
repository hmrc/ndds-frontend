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

import models.SuspensionPeriodRange
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class SuspensionPeriodRangeDateFormProviderSpec extends AnyWordSpec with Matchers {

  implicit val messages: Messages = stubMessages()

  private val today = LocalDate.of(2025, 11, 1)
  private val earliestStart = today.plusDays(3)
  private val planStart = LocalDate.of(2025, 11, 10)
  private val planEnd = LocalDate.of(2026, 1, 31)

  private val formProvider = new SuspensionPeriodRangeDateFormProvider()
  private val form = formProvider(Some(planStart), Some(planEnd), earliestStart)

  private val validStartDate = planStart.plusDays(2)
  private val validEndDate = validStartDate.plusDays(10)

  "SuspensionPeriodRangeDateFormProvider" should {

    "bind valid dates correctly" in {
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

    "fail when start date is before plan start date" in {
      val invalidStart = planStart.minusDays(1)

      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> invalidStart.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> invalidStart.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> invalidStart.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> validEndDate.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> validEndDate.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> validEndDate.getYear.toString
      )

      val result = form.bind(data)
      result.errors.find(_.key == "suspensionPeriodRangeStartDate") mustBe defined
      result.errors.find(_.key == "suspensionPeriodRangeStartDate").map(_.message) must contain("suspensionPeriodRangeDate.error.startDate")
    }

    "fail when start date is before earliest allowed start date" in {
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
      result.errors.find(_.key == "suspensionPeriodRangeStartDate") mustBe defined
      result.errors.find(_.key == "suspensionPeriodRangeStartDate").map(_.message) must contain("suspensionPeriodRangeDate.error.startDate")
    }

    "fail when start date is after plan end date" in {
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
      result.errors.find(_.key == "suspensionPeriodRangeStartDate") mustBe defined
      result.errors.find(_.key == "suspensionPeriodRangeStartDate").map(_.message) must contain("suspensionPeriodRangeDate.error.startDate")
    }

    "fail when end date is before start date" in {
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
      result.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate") mustBe defined
      result.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate").map(_.message) must contain(
        "suspensionPeriodRangeDate.error.endDate"
      )
    }

    "fail when end date is after plan end date" in {
      val invalidEnd = planEnd.plusDays(2)

      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
        "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
        "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
        "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
        "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
        "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
      )

      val result = form.bind(data)
      result.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate") mustBe defined
      result.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate").map(_.message) must contain(
        "suspensionPeriodRangeDate.error.endDate"
      )
    }

    "should fail when start date format is invalid" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "xx",
        "suspensionPeriodRangeStartDate.month" -> "13",
        "suspensionPeriodRangeStartDate.year"  -> "abcd",
        "suspensionPeriodRangeEndDate.day"     -> "10",
        "suspensionPeriodRangeEndDate.month"   -> "12",
        "suspensionPeriodRangeEndDate.year"    -> "2025"
      )

      val result = form.bind(data)
      val expectedErrors = Seq(
        FormError("suspensionPeriodRangeStartDate", "suspensionPeriodRangeDate.error.invalid.startDate.base", Seq()),
        FormError("suspensionPeriodRangeStartDate", "suspensionPeriodRangeDate.error.invalid.startDate.base", Seq()),
        FormError("suspensionPeriodRangeStartDate", "suspensionPeriodRangeDate.error.invalid.startDate.base", Seq())
      )
      result.errors must contain theSameElementsAs expectedErrors
    }

    "should fail when end date format is invalid" in {
      val data = Map(
        "suspensionPeriodRangeStartDate.day"   -> "10",
        "suspensionPeriodRangeStartDate.month" -> "11",
        "suspensionPeriodRangeStartDate.year"  -> "2025",
        "suspensionPeriodRangeEndDate.day"     -> "xx",
        "suspensionPeriodRangeEndDate.month"   -> "13",
        "suspensionPeriodRangeEndDate.year"    -> "abcd"
      )

      val result = form.bind(data)
      val expectedErrors = Seq(
        FormError("suspensionPeriodRangeEndDate", "suspensionPeriodRangeDate.error.invalid.endDate.base", Seq()),
        FormError("suspensionPeriodRangeEndDate", "suspensionPeriodRangeDate.error.invalid.endDate.base", Seq()),
        FormError("suspensionPeriodRangeEndDate", "suspensionPeriodRangeDate.error.invalid.endDate.base", Seq())
      )
      result.errors must contain theSameElementsAs expectedErrors
    }

    "withMappedErrors" should {

      "map end date constraint error to field key" in {
        val invalidEnd = validStartDate.minusDays(1)

        val data = Map(
          "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
          "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
          "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
          "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
          "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
          "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
        )

        val boundForm = form.bind(data)
        val mappedForm = formProvider.withMappedErrors(boundForm)

        mappedForm.errors.find(_.key == "suspensionPeriodRangeEndDate") mustBe defined
        mappedForm.errors.find(_.key == "suspensionPeriodRangeEndDate").map(_.message) must contain("suspensionPeriodRangeDate.error.endDate")
        mappedForm.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate") mustBe empty
      }

      "preserve other errors when mapping" in {
        val validStartForConstraint = validStartDate
        val invalidEnd = validStartForConstraint.minusDays(1)

        val data = Map(
          "suspensionPeriodRangeStartDate.day"   -> validStartForConstraint.getDayOfMonth.toString,
          "suspensionPeriodRangeStartDate.month" -> validStartForConstraint.getMonthValue.toString,
          "suspensionPeriodRangeStartDate.year"  -> validStartForConstraint.getYear.toString,
          "suspensionPeriodRangeEndDate.day"     -> invalidEnd.getDayOfMonth.toString,
          "suspensionPeriodRangeEndDate.month"   -> invalidEnd.getMonthValue.toString,
          "suspensionPeriodRangeEndDate.year"    -> invalidEnd.getYear.toString
        )

        val boundForm = form.bind(data)
        val startDateError = boundForm.errors.find(_.key == "suspensionPeriodRangeStartDate")
        val endDateConstraintError = boundForm.errors.find(e => e.key.isEmpty || e.key == "suspensionPeriodRangeDate.error.endDate")

        val mappedForm = formProvider.withMappedErrors(boundForm)

        if (startDateError.isDefined) {
          mappedForm.errors.find(_.key == "suspensionPeriodRangeStartDate") mustBe defined
        }
        if (endDateConstraintError.isDefined) {
          mappedForm.errors.find(_.key == "suspensionPeriodRangeEndDate") mustBe defined
        }
      }

      "return form unchanged when no constraint errors" in {
        val data = Map(
          "suspensionPeriodRangeStartDate.day"   -> validStartDate.getDayOfMonth.toString,
          "suspensionPeriodRangeStartDate.month" -> validStartDate.getMonthValue.toString,
          "suspensionPeriodRangeStartDate.year"  -> validStartDate.getYear.toString,
          "suspensionPeriodRangeEndDate.day"     -> validEndDate.getDayOfMonth.toString,
          "suspensionPeriodRangeEndDate.month"   -> validEndDate.getMonthValue.toString,
          "suspensionPeriodRangeEndDate.year"    -> validEndDate.getYear.toString
        )

        val boundForm = form.bind(data)
        val mappedForm = formProvider.withMappedErrors(boundForm)

        mappedForm.errors mustBe empty
        mappedForm.value mustBe boundForm.value
      }

    }

  }
}
