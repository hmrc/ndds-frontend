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
import play.api.i18n.Messages
import utils.DateFormats

import java.time.LocalDate
import javax.inject.Inject

class SuspensionPeriodRangeDateFormProvider @Inject() extends Mappings {
  def apply(
    planStartDateOpt: Option[LocalDate],
    planEndDateOpt: Option[LocalDate],
    earliestStartDate: LocalDate
  )(implicit messages: Messages): Form[SuspensionPeriodRange] = {

    val latestStartDate = LocalDate.now().plusMonths(6)

    Form(
      mapping(
        "suspensionPeriodRangeStartDate" -> customPaymentDate(
          invalidKey     = "suspensionPeriodRangeStartDate.error.invalid",
          allRequiredKey = "suspensionPeriodRangeStartDate.error.required.all",
          twoRequiredKey = "suspensionPeriodRangeStartDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeStartDate.error.required",
          dateFormats    = DateFormats.defaultDateFormats
        ),
        "suspensionPeriodRangeEndDate" -> customPaymentDate(
          invalidKey     = "suspensionPeriodRangeEndDate.error.invalid",
          allRequiredKey = "suspensionPeriodRangeEndDate.error.required.all",
          twoRequiredKey = "suspensionPeriodRangeEndDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeEndDate.error.required",
          dateFormats    = DateFormats.defaultDateFormats
        )
      )(SuspensionPeriodRange.apply)(range => Some((range.startDate, range.endDate)))

        // Suspend start date must be on or after plan start date ** comments will be removed later once local testing done
        .verifying(
          "suspensionPeriodRangeDate.error.startBeforePlanStart",
          range => planStartDateOpt.forall(planStart => !range.startDate.isBefore(planStart))
        )

        // Suspend start date must be on or before plan end date (if exists)
        .verifying(
          "suspensionPeriodRangeDate.error.startAfterPlanEnd",
          range => planEndDateOpt.forall(planEnd => !range.startDate.isAfter(planEnd))
        )

        // Suspend start date must be no earlier than 3 working days from today
        .verifying(
          "suspensionPeriodRangeDate.error.startBeforeEarliestAllowed",
          range => !range.startDate.isBefore(earliestStartDate)
        )

        // Suspend start date must be no later than 6 months from today
        .verifying(
          "suspensionPeriodRangeDate.error.startAfterLatestAllowed",
          range => !range.startDate.isAfter(latestStartDate)
        )

        // Suspend end date must be on or after suspend start date
        .verifying(
          "suspensionPeriodRangeDate.error.endBeforeStart",
          range => !range.endDate.isBefore(range.startDate)
        )

        // Suspend end date must be no later than 6 months from the suspend start date
        .verifying(
          "suspensionPeriodRangeDate.error.endAfterLatestAllowed",
          range => !range.endDate.isAfter(range.startDate.plusMonths(6))
        )

        // Suspend end date must be on or before plan end date (if exists)
        .verifying(
          "suspensionPeriodRangeDate.error.endAfterPlanEnd",
          range => planEndDateOpt.forall(planEnd => !range.endDate.isAfter(planEnd))
        )
    )
  }
}
