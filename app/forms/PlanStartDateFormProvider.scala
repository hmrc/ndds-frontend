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

import forms.mappings.{DateFormat, Mappings}
import models.UserAnswers
import play.api.data.Form
import play.api.i18n.Messages
import utils.DateFormats

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PlanStartDateFormProvider @Inject() extends Mappings {

  private val DayRegex: String = "^([1-9]|0[1-9]|1[0-9]|2[0-9]|3[0-1])$"
  private val MonthRegex: String = "^(0?[1-9]|1[0-2])$"
  private val YearRegex: String = "^\\d{4}$"

  private val paymentStartDateFormats: Seq[DateFormat] = Seq(
    DateFormat("day", "planStartDate.error.invalid", DayRegex),
    DateFormat("month", "planStartDate.error.invalid", MonthRegex),
    DateFormat("year", "planStartDate.error.invalid", YearRegex)
  )

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> customPaymentDate(
        invalidKey     = "planStartDate.error.invalid",
        allRequiredKey = "planStartDate.error.required.all",
        twoRequiredKey = "planStartDate.error.required.two",
        requiredKey    = "planStartDate.error.required",
        dateFormats    = DateFormats.defaultDateFormats
      )
    )

  def apply(userAnswers: UserAnswers, earliestPlanStartDate: LocalDate)(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> planStartDate(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = messages("planStartDate.error.beforeEarliestDate", earliestPlanStartDate.format(dateTimeFormatter)),
        budgetAfterMaxDateKey    = messages("planStartDate.error.budgetAfterMaxDate", earliestPlanStartDate.format(dateTimeFormatter)),
        timeToPayAfterMaxDateKey = messages("planStartDate.error.timeToPayAfterMaxDate", earliestPlanStartDate.format(dateTimeFormatter)),
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = userAnswers,
        earliestPlanStartDate    = earliestPlanStartDate
      )
    )
}
