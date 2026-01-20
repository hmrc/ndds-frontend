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
import play.api.data.Form
import play.api.i18n.Messages
import utils.DateFormats

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import javax.inject.Inject

class PaymentDateFormProvider @Inject() (clock: Clock) extends Mappings {

  private val DayRegex: String = "^([1-9]|0[1-9]|1[0-9]|2[0-9]|3[0-1])$"
  private val MonthRegex: String = "^(0?[1-9]|1[0-2])$"
  private val YearRegex: String = "^\\d{4}$"

  private val paymentDateFormats: Seq[DateFormat] = Seq(
    DateFormat("day", "paymentDate.error.invalid", DayRegex),
    DateFormat("month", "paymentDate.error.invalid", MonthRegex),
    DateFormat("year", "paymentDate.error.invalid", YearRegex)
  )

  def apply(earliestDate: LocalDate, isSinglePlan: Boolean)(implicit messages: Messages): Form[LocalDate] = {

    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val today = LocalDate.now(clock)
    val maxDateForSinglePlan = today.plusYears(1)

    Form(
      "value" -> customPaymentDate(
        invalidKey     = "paymentDate.error.invalid",
        allRequiredKey = "paymentDate.error.required.all",
        twoRequiredKey = "paymentDate.error.required.two",
        requiredKey    = "paymentDate.error.required",
        dateFormats    = paymentDateFormats
      )
        .verifying(
          messages("paymentDate.error.beforeEarliest", earliestDate.format(dateTimeFormatter)),
          date => !date.isBefore(earliestDate)
        )
        .verifying(
          messages("paymentDate.error.tooFarInFuture", maxDateForSinglePlan.format(dateTimeFormatter)),
          date => !isSinglePlan || !date.isAfter(maxDateForSinglePlan)
        )
    )
  }
}
