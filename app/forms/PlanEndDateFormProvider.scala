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

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PlanEndDateFormProvider @Inject() extends Mappings {

  def apply(startDate: LocalDate)(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> optionalLocalDate(
        invalidKey     = "planEndDate.error.invalid",
        allRequiredKey = "planEndDate.error.required.all",
        twoRequiredKey = "planEndDate.error.required.two",
        requiredKey    = "planEndDate.error.required"
      ).verifying(dateAfter(startDate, "planEndDate.error.beforeOrEqualStartDate"))
    )

  private def dateAfter(start: LocalDate, errorKey: String): Constraint[LocalDate] = {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    Constraint { endDate =>
      if (!endDate.isAfter(start.minusDays(1))) {
        Invalid(ValidationError(errorKey, start.format(formatter)))
      } else {
        Valid
      }
    }
  }
}
