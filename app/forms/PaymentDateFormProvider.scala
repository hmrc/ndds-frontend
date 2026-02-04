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

import forms.mappings.{DateFormat, Mappings}
import play.api.data.Form
import play.api.i18n.Messages
import utils.DateFormats

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import javax.inject.Inject

class PaymentDateFormProvider @Inject() (clock: Clock) extends Mappings {

  def apply(earliestDate: LocalDate, isSinglePlan: Boolean)(implicit messages: Messages): Form[LocalDate] = {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

    val today = LocalDate.now(clock)
    val maxDateForSinglePlan = today.plusYears(1)

    Form(
      "value" -> customPaymentDate(
        invalidKey     = "paymentDate.error.invalid",
        allRequiredKey = "paymentDate.error.required.all",
        twoRequiredKey = "paymentDate.error.required.two",
        requiredKey    = "paymentDate.error.required",
        dateFormats    = DateFormats.defaultDateFormats
      )
        .verifying(
          messages("paymentDate.error.beforeEarliest", earliestDate.format(formatter)),
          date => !date.isBefore(earliestDate)
        )
        .verifying(
          messages("paymentDate.error.tooFarInFuture", maxDateForSinglePlan.format(formatter)),
          date => !isSinglePlan || !date.isAfter(maxDateForSinglePlan)
        )
    )
  }
}
