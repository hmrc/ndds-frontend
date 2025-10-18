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

import javax.inject.Inject

class SuspensionPeriodRangeDateFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[SuspensionPeriodRange] =
    Form(
      mapping(
        "suspensionPeriodRangeStartDate" -> localDate(
          invalidKey     = "suspensionPeriodRangeStartDate.error.invalid",
          allRequiredKey = "suspensionPeriodRangeStartDate.error.required",
          twoRequiredKey = "suspensionPeriodRangeStartDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeStartDate.error.required"
        ),
        "suspensionPeriodRangeEndDate" -> localDate(
          invalidKey     = "suspensionPeriodRangeEndDate.error.invalid",
          allRequiredKey = "suspensionPeriodRangeEndDate.error.required",
          twoRequiredKey = "suspensionPeriodRangeEndDate.error.required.two",
          requiredKey    = "suspensionPeriodRangeEndDate.error.required"
        )
      )(
        SuspensionPeriodRange.apply
      )(range => Some((range.startDate, range.endDate)))
        .verifying(
          "suspensionPeriodRangeDate.error.endBeforeStart",
          range => !range.endDate.isBefore(range.startDate)
        )
    )
}
