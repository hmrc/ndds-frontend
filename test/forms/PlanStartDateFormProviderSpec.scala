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
import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import org.scalatest.TryValues
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PlanEndDatePage}
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.{LocalDate, ZoneOffset}

class PlanStartDateFormProviderSpec extends DateBehaviours with TryValues {

  private implicit val messages: Messages = stubMessages()
  private val form = new PlanStartDateFormProvider()()

  ".value" - {

    val validData = datesBetween(
      min = LocalDate.of(2000, 1, 1),
      max = LocalDate.now(ZoneOffset.UTC)
    )

    behave like dateField(form, "value", validData)

    "fail to bind an empty date" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain theSameElementsAs Seq(
        FormError("value.day", "date.error.day"),
        FormError("value.month", "date.error.month"),
        FormError("value.year", "date.error.year")
      )
    }
  }

  ".value with business rules" - {

    val earliestPlanStartDate = LocalDate.of(2024, 1, 15)
    val userAnswers = UserAnswers("test-id")

    "must bind valid date successfully" in {
      val formWithRules = new PlanStartDateFormProvider().apply(userAnswers, earliestPlanStartDate)
      val data = Map(
        "value.day"   -> "20",
        "value.month" -> "1",
        "value.year"  -> "2024"
      )

      val result = formWithRules.bind(data)
      result.get mustBe LocalDate.of(2024, 1, 20)
      result.errors mustBe empty
    }

    "must return error when date is before earliest plan start date" in {
      val formWithRules = new PlanStartDateFormProvider().apply(userAnswers, earliestPlanStartDate)
      val data = Map(
        "value.day"   -> "10",
        "value.month" -> "1",
        "value.year"  -> "2024"
      )

      val result = formWithRules.bind(data)
      result.errors must contain(FormError("value", "planStartDate.error.beforeEarliestDate"))
    }

    "must return error for BUDGET payment plan when date is more than 12 months after current date" in {
      val currentDate = LocalDate.now()
      val budgetUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.SA)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
        .success
        .value

      val formWithRules = new PlanStartDateFormProvider().apply(budgetUserAnswers, earliestPlanStartDate)
      val futureDate = currentDate.plusYears(1).plusDays(1)
      val data = Map(
        "value.day"   -> futureDate.getDayOfMonth.toString,
        "value.month" -> futureDate.getMonthValue.toString,
        "value.year"  -> futureDate.getYear.toString
      )

      val result = formWithRules.bind(data)
      result.errors must contain(FormError("value", "planStartDate.error.budgetAfterMaxDate"))
    }

    "must return error for TIME_TO_PAY/VPP payment plan when date is more than 30 days after current date" in {
      val currentDate = LocalDate.now()
      val vppUserAnswers = UserAnswers("test-id")
        .set(DirectDebitSourcePage, DirectDebitSource.MGD)
        .success
        .value
        .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
        .success
        .value

      val formWithRules = new PlanStartDateFormProvider().apply(vppUserAnswers, earliestPlanStartDate)
      val futureDate = currentDate.plusDays(31)
      val data = Map(
        "value.day"   -> futureDate.getDayOfMonth.toString,
        "value.month" -> futureDate.getMonthValue.toString,
        "value.year"  -> futureDate.getYear.toString
      )

      val result = formWithRules.bind(data)
      result.errors must contain(FormError("value", "planStartDate.error.timeToPayAfterMaxDate"))
    }

    "fail binding when start date is after end date" in {
      val userAnswers =
        UserAnswers("id")
          .set(PlanEndDatePage, LocalDate.of(2025, 1, 10))
          .success
          .value

      val form = new PlanStartDateFormProvider().apply(userAnswers, LocalDate.of(2024, 1, 1))

      val boundForm = form.bind(
        Map("value.day" -> "15", "value.month" -> "1", "value.year" -> "2025")
      )

      boundForm.errors                must contain(FormError("value", "planStartDate.error.AfterOrEqualEndDate"))
      boundForm.errors.map(_.message) must contain("planStartDate.error.AfterOrEqualEndDate")
    }

  }

}
