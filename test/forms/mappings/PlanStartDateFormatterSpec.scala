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

package forms.mappings

import base.SpecBase
import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import pages.{DirectDebitSourcePage, PaymentPlanTypePage}
import play.api.data.FormError
import play.api.test.Helpers.stubMessages
import utils.DateFormats

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlanStartDateFormatterSpec extends SpecBase {

  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  private val formatter = new PlanStartDateFormatter(
    invalidKey               = "planStartDate.error.invalid",
    allRequiredKey           = "planStartDate.error.required.all",
    twoRequiredKey           = "planStartDate.error.required.two",
    requiredKey              = "planStartDate.error.required",
    beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
    budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
    timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
    dateFormats              = DateFormats.defaultDateFormats,
    userAnswers              = UserAnswers("test-id"),
    earliestPlanStartDate    = LocalDate.of(2024, 1, 15)
  )(stubMessages())

  "PlanStartDateFormatter" - {

    "must bind valid date successfully" in {
      val data = Map(
        "value.day"   -> "20",
        "value.month" -> "1",
        "value.year"  -> "2024"
      )

      val result = formatter.bind("value", data)
      result mustBe Right(LocalDate.of(2024, 1, 20))
    }

    "must return error when date is before earliest plan start date" in {
      val earliest = LocalDate.of(2024, 1, 15)
      val data = Map(
        "value.day"   -> "10",
        "value.month" -> "1",
        "value.year"  -> "2024"
      )

      val result = formatter.bind("value", data)
      result mustBe Left(
        Seq(FormError("value", "planStartDate.error.beforeEarliestDate", Seq(earliest.format(dateFormatter))))
      )
    }

    "must return error for BUDGET payment plan when date is more than 12 months after current date" in {
      val currentDate = LocalDate.now()
      val expectedMax = currentDate.plusYears(1)
      val budgetUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.SA)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .success
        .value

      val formatterWithBudget = new PlanStartDateFormatter(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
        budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
        timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = budgetUserAnswers,
        earliestPlanStartDate    = LocalDate.of(2024, 1, 15)
      )(stubMessages())

      val futureDate = currentDate.plusYears(1).plusDays(1)
      val data = Map(
        "value.day"   -> futureDate.getDayOfMonth.toString,
        "value.month" -> futureDate.getMonthValue.toString,
        "value.year"  -> futureDate.getYear.toString
      )

      val result = formatterWithBudget.bind("value", data)
      result mustBe Left(
        Seq(FormError("value", "planStartDate.error.budgetAfterMaxDate", Seq(expectedMax.format(dateFormatter))))
      )
    }

    "must return error for TIME_TO_PAY/VPP payment plan when date is more than 30 days after current date" in {
      val currentDate = LocalDate.now()
      val expectedMax = currentDate.plusDays(30)
      val vppUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.MGD)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
        .success
        .value

      val formatterWithVpp = new PlanStartDateFormatter(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
        budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
        timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = vppUserAnswers,
        earliestPlanStartDate    = LocalDate.of(2024, 1, 15)
      )(stubMessages())

      val futureDate = currentDate.plusDays(31)
      val data = Map(
        "value.day"   -> futureDate.getDayOfMonth.toString,
        "value.month" -> futureDate.getMonthValue.toString,
        "value.year"  -> futureDate.getYear.toString
      )

      val result = formatterWithVpp.bind("value", data)
      result mustBe Left(
        Seq(FormError("value", "planStartDate.error.timeToPayAfterMaxDate", Seq(expectedMax.format(dateFormatter))))
      )
    }

    "must allow valid date for BUDGET payment plan within 12 months" in {
      val currentDate = LocalDate.now()
      val budgetUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.SA)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .success
        .value

      val formatterWithBudget = new PlanStartDateFormatter(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
        budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
        timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = budgetUserAnswers,
        earliestPlanStartDate    = LocalDate.of(2024, 1, 15)
      )(stubMessages())

      val validDate = currentDate.plusMonths(6)
      val data = Map(
        "value.day"   -> validDate.getDayOfMonth.toString,
        "value.month" -> validDate.getMonthValue.toString,
        "value.year"  -> validDate.getYear.toString
      )

      val result = formatterWithBudget.bind("value", data)
      result mustBe Right(validDate)
    }

    "must allow valid date for TIME_TO_PAY/VPP payment plan within 30 days" in {
      val currentDate = LocalDate.now()
      val vppUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.MGD)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
        .success
        .value

      val formatterWithVpp = new PlanStartDateFormatter(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
        budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
        timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = vppUserAnswers,
        earliestPlanStartDate    = LocalDate.of(2024, 1, 15)
      )(stubMessages())

      val validDate = currentDate.plusDays(15)
      val data = Map(
        "value.day"   -> validDate.getDayOfMonth.toString,
        "value.month" -> validDate.getMonthValue.toString,
        "value.year"  -> validDate.getYear.toString
      )

      val result = formatterWithVpp.bind("value", data)
      result mustBe Right(validDate)
    }
  }
}
