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

import forms.mappings.Mappings
import models.SuspensionPeriodRange
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import utils.DateFormats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SuspensionPeriodRangeDateFormProvider @Inject() extends Mappings {

  private val MaxMonthsAhead = 6
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def apply(
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earliestStartDate: LocalDate // 3 working days from today
  )(implicit messages: Messages): Form[SuspensionPeriodRange] = {

    Form(
      mapping(
        "suspensionPeriodRangeStartDate" -> customPaymentDate(
          invalidKey     = "suspensionPeriodRangeDate.error.invalid.startDate.base",
          allRequiredKey = "suspensionPeriodRangeStartDate.error.required.all",
          twoRequiredKey = "suspensionPeriodRangeStartDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeStartDate.error.required",
          dateFormats    = DateFormats.defaultDateFormats
        ),
        "suspensionPeriodRangeEndDate" -> customPaymentDate(
          invalidKey     = "suspensionPeriodRangeDate.error.invalid.endDate.base",
          allRequiredKey = "suspensionPeriodRangeEndDate.error.required.all",
          twoRequiredKey = "suspensionPeriodRangeEndDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeEndDate.error.required",
          dateFormats    = DateFormats.defaultDateFormats
        )
      )(SuspensionPeriodRange.apply)(range => Some((range.startDate, range.endDate)))
        .verifying(startDateConstraint(planStartDateOpt, planEndDateOpt, earliestStartDate))
        .verifying(endDateConstraint(planStartDateOpt, planEndDateOpt, earliestStartDate))
    )
  }

  private def isSuspendStartDateValid(
    startDate: LocalDate,
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earliestStartDate: LocalDate
  ): Boolean = {
    val lowerBound = planStartDateOpt.fold(earliestStartDate)(psd => if (psd.isAfter(earliestStartDate)) psd else earliestStartDate)
    val upperBound = planEndDateOpt.fold(LocalDate.now().plusMonths(MaxMonthsAhead)) { ped =>
      val sixMonthsFromToday = LocalDate.now().plusMonths(MaxMonthsAhead)
      if (ped.isBefore(sixMonthsFromToday)) ped else sixMonthsFromToday
    }
    !startDate.isBefore(lowerBound) && !startDate.isAfter(upperBound)
  }

  private def isSuspendEndDateValid(
    endDate: LocalDate,
    startDate: LocalDate,
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate]
  ): Boolean = {
    val lowerBound = planStartDateOpt.fold(startDate)(psd => if (psd.isAfter(startDate)) psd else startDate)
    val upperBound = planEndDateOpt.fold(startDate.plusMonths(MaxMonthsAhead)) { ped =>
      val sixMonthsFromStart = startDate.plusMonths(MaxMonthsAhead)
      if (ped.isBefore(sixMonthsFromStart)) ped else sixMonthsFromStart
    }
    !endDate.isBefore(lowerBound) && !endDate.isAfter(upperBound)
  }

  private def startDateConstraint(
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earliestStartDate: LocalDate
  )(implicit messages: Messages): Constraint[SuspensionPeriodRange] =
    Constraint[SuspensionPeriodRange]("suspensionPeriodRangeDate.error.startDate") { range =>
      val lowerBound = planStartDateOpt.fold(earliestStartDate)(psd => if (psd.isAfter(earliestStartDate)) psd else earliestStartDate)
      val upperBound = planEndDateOpt.fold(LocalDate.now().plusMonths(MaxMonthsAhead)) { ped =>
        val sixMonthsFromToday = LocalDate.now().plusMonths(MaxMonthsAhead)
        if (ped.isBefore(sixMonthsFromToday)) ped else sixMonthsFromToday
      }
      if (isSuspendStartDateValid(range.startDate, planStartDateOpt, planEndDateOpt, earliestStartDate)) Valid
      else Invalid(messages("suspensionPeriodRangeDate.error.startDate", lowerBound.format(dateFormatter), upperBound.format(dateFormatter)))
    }

  private def endDateConstraint(
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earlyStartDate: LocalDate
  )(implicit messages: Messages): Constraint[SuspensionPeriodRange] =
    Constraint[SuspensionPeriodRange]("suspensionPeriodRangeDate.error.endDate") { range =>

      val startValid = isSuspendStartDateValid(
        range.startDate,
        planStartDateOpt,
        planEndDateOpt,
        earlyStartDate
      )

      if (!startValid) {
        Valid
      } else {

        val lowerBound = planStartDateOpt.fold(range.startDate)(psd => if (psd.isAfter(range.startDate)) psd else range.startDate)
        val upperBound = planEndDateOpt.fold(range.startDate.plusMonths(MaxMonthsAhead)) { ped =>
          val SixMonthsFromSuspendStartDate = range.startDate.plusMonths(MaxMonthsAhead)
          if (ped.isBefore(SixMonthsFromSuspendStartDate)) ped else SixMonthsFromSuspendStartDate
        }

        if (
          !range.endDate.isBefore(range.startDate) &&
          isSuspendEndDateValid(range.endDate, range.startDate, planStartDateOpt, planEndDateOpt)
        ) {
          Valid
        } else {
          Invalid(messages("suspensionPeriodRangeDate.error.endDate", lowerBound.format(dateFormatter), upperBound.format(dateFormatter)))
        }
      }
    }

}
