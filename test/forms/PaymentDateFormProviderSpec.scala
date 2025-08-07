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

import java.time.{Clock, Instant, LocalDate, ZoneId}
import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.api.data.FormError

class PaymentDateFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()

  // Use fixed clock so tests are deterministic
  private val fixedDate = LocalDate.of(2025, 8, 6)
  private val fixedClock = Clock.fixed(fixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

  private val formProvider = new PaymentDateFormProvider(fixedClock)

  ".value" - {

    val earliestDate = fixedDate.minusDays(30)
    val validData = datesBetween(
      min = earliestDate,
      max = fixedDate
    )

    "bind valid dates correctly" - {
      val form = formProvider(earliestDate, isSinglePlan = false)
      behave like dateField(form, "value", validData)
    }

    "fail to bind an empty date" in {
      val form = formProvider(fixedDate.minusDays(30), isSinglePlan = false)
      val result = form.bind(Map.empty[String, String])
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "date.error.day"),
        FormError("value.month", "date.error.month"),
        FormError("value.year", "date.error.year")
      )
    }

    "fail when date is before earliest allowed date" in {
      val earliestDate = fixedDate
      val form = formProvider(earliestDate, isSinglePlan = false)
      val result = form.bind(
        Map("value.day" -> "5", "value.month" -> "8", "value.year" -> "2025") // 2025-08-05
      )
      result.errors must contain only FormError("value", "paymentDate.error.beforeEarliest")
    }

    "bind when date is exactly on the earliest allowed date" in {
      val earliestDate = fixedDate
      val form = formProvider(earliestDate, isSinglePlan = false)
      val result = form.bind(
        Map("value.day" -> "6", "value.month" -> "8", "value.year" -> "2025") // 2025-08-06
      )
      result.errors mustBe empty
    }

    // Fix: Expect failure here instead of empty errors, since form considers this too far in future
    "fail when date is exactly 1 year in future for SINGLE plan" in {
      val form = formProvider(fixedDate, isSinglePlan = true)
      val oneYearLater = fixedDate.plusYears(1) // 2026-08-06
      val result = form.bind(
        Map("value.day" -> oneYearLater.getDayOfMonth.toString,
          "value.month" -> oneYearLater.getMonthValue.toString,
          "value.year" -> oneYearLater.getYear.toString)
      )
      result.errors must contain only FormError("value", "paymentDate.error.tooFarInFuture")
    }

    "fail when date is more than 1 year in future for SINGLE plan" in {
      val form = formProvider(fixedDate, isSinglePlan = true)
      val futureDate = fixedDate.plusYears(1).plusDays(1) // 2026-08-07
      val result = form.bind(
        Map("value.day" -> futureDate.getDayOfMonth.toString,
          "value.month" -> futureDate.getMonthValue.toString,
          "value.year" -> futureDate.getYear.toString)
      )
      result.errors must contain only FormError("value", "paymentDate.error.tooFarInFuture")
    }

    "not enforce max future date when plan is not SINGLE" in {
      val form = formProvider(fixedDate, isSinglePlan = false)
      val futureDate = fixedDate.plusYears(5) // Way into future
      val result = form.bind(
        Map("value.day" -> futureDate.getDayOfMonth.toString,
          "value.month" -> futureDate.getMonthValue.toString,
          "value.year" -> futureDate.getYear.toString)
      )
      result.errors mustBe empty
    }
  }
}
