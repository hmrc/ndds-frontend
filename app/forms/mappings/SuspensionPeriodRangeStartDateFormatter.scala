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

import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SuspensionPeriodRangeStartDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  args: Seq[String] = Seq.empty,
  dateFormats: Seq[DateFormat],
  planStartDateOpt: Option[LocalDate],
  planEndDateOpt: Option[LocalDate],
  earliestStartDate: LocalDate
)(implicit messages: Messages)
    extends CustomDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args, dateFormats) {

  private val MaxMonthsAhead = 6
  private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", messages.lang.locale)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    super.bind(key, data).flatMap { date =>
      validateBusinessRules(key, date)
    }
  }

  private def validateBusinessRules(key: String, enteredDate: LocalDate): Either[Seq[FormError], LocalDate] = {
    val lowerBound = planStartDateOpt.fold(earliestStartDate)(psd => if (psd.isAfter(earliestStartDate)) psd else earliestStartDate)
    val upperBound = planEndDateOpt.fold(LocalDate.now().plusMonths(MaxMonthsAhead)) { ped =>
      val sixMonthsFromToday = LocalDate.now().plusMonths(MaxMonthsAhead)
      if (ped.isBefore(sixMonthsFromToday)) ped else sixMonthsFromToday
    }

    if (enteredDate.isBefore(lowerBound) || enteredDate.isAfter(upperBound)) {
      Left(
        Seq(
          FormError(
            key,
            "suspensionPeriodRangeDate.error.startDate",
            Seq(lowerBound.format(dateFormatter), upperBound.format(dateFormatter))
          )
        )
      )
    } else {
      Right(enteredDate)
    }
  }
}
