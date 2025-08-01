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
import play.api.data.Form
import play.api.data.validation._
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class PlanEndDateFormProvider @Inject() extends Mappings {

  def apply(startDate: LocalDate)(implicit messages: Messages): Form[Option[LocalDate]] =
    Form(
      "value" -> optionalLocalDate(
        invalidKey     = "planEndDate.error.invalid",
        allRequiredKey = "planEndDate.error.incomplete",
        twoRequiredKey = "planEndDate.error.incomplete",
        requiredKey    = "planEndDate.error.incomplete"
      ).verifying(optionalDateAfter(startDate, "planEndDate.error.beforeOrEqualStartDate"))
    )

  private def optionalDateAfter(start: LocalDate, errorKey: String): Constraint[Option[LocalDate]] =
    Constraint {
      case Some(endDate) if endDate.isBefore(start) =>
        Invalid(ValidationError(errorKey))
      case _ =>
        Valid
    }
}

