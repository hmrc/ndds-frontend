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
    earliestStartDate: LocalDate
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
        .verifying(endDateConstraint(planEndDateOpt))
    )
  }

  private def isSuspendStartDateValid(
    startDate: LocalDate,
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earliestStartDate: LocalDate
  ): Boolean = {
    val latestStartDate = LocalDate.now().plusMonths(MaxMonthsAhead)

    val afterPlanStart = planStartDateOpt.forall(planStart => !startDate.isBefore(planStart))
    val beforePlanEnd = planEndDateOpt.forall(planEnd => !startDate.isAfter(planEnd))
    val afterEarliest = !startDate.isBefore(earliestStartDate)
    val beforeLatest = !startDate.isAfter(latestStartDate)

    afterPlanStart && beforePlanEnd && afterEarliest && beforeLatest
  }

  private def isSuspendEndDateValid(
    endDate: LocalDate,
    startDate: LocalDate,
    planEndDateOpt: Option[LocalDate]
  ): Boolean = {
    val latestAllowedEndDate = LocalDate.now().plusMonths(MaxMonthsAhead)
    val within6Months = !endDate.isAfter(latestAllowedEndDate)
    val afterStart = !endDate.isBefore(startDate)
    val beforePlanEnd = planEndDateOpt.forall(planEnd => !endDate.isAfter(planEnd))

    afterStart && within6Months && beforePlanEnd
  }

  private def startDateConstraint(planStartDateOpt: Option[LocalDate], planEndDateOpt: Option[LocalDate], earliestStartDate: LocalDate)(implicit
    messages: Messages
  ): Constraint[SuspensionPeriodRange] =
    Constraint[SuspensionPeriodRange]("suspensionPeriodRangeDate.error.startDate") { range =>
      if (isSuspendStartDateValid(range.startDate, planStartDateOpt, planEndDateOpt, earliestStartDate)) Valid
      else
        Invalid(
          messages(
            "suspensionPeriodRangeDate.error.startDate",
            earliestStartDate.format(dateFormatter),
            planStartDateOpt.map(_.format(dateFormatter)).getOrElse(""),
            LocalDate.now().plusMonths(MaxMonthsAhead).format(dateFormatter),
            planEndDateOpt.map(_.format(dateFormatter)).getOrElse("")
          )
        )
    }

  private def endDateConstraint(planEndDateOpt: Option[LocalDate])(implicit messages: Messages): Constraint[SuspensionPeriodRange] =
    Constraint[SuspensionPeriodRange]("suspensionPeriodRangeDate.error.endDate") { range =>
      if (isSuspendEndDateValid(range.endDate, range.startDate, planEndDateOpt)) Valid
      else
        Invalid(
          messages(
            "suspensionPeriodRangeDate.error.endDate",
            range.startDate.format(dateFormatter),
            formatUpperBoundEnd(planEndDateOpt)
          )
        )
    }

  private def formatUpperBoundEnd(planEndDateOpt: Option[LocalDate]): String =
    planEndDateOpt.map(_.format(dateFormatter)).getOrElse(LocalDate.now().plusMonths(MaxMonthsAhead).format(dateFormatter))
}
