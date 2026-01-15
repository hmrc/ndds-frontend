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
import models.UserAnswers
import pages.PlanEndDatePage
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import utils.DateFormats

import java.time.LocalDate
import javax.inject.Inject

class PlanStartDateFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] = {
    Form(
      "value" -> customPaymentDate(
        invalidKey     = "planStartDate.error.invalid",
        allRequiredKey = "planStartDate.error.required.all",
        twoRequiredKey = "planStartDate.error.required.two",
        requiredKey    = "planStartDate.error.required",
        dateFormats    = DateFormats.defaultDateFormats
      )
    )
  }

  def apply(userAnswers: UserAnswers, earliestPlanStartDate: LocalDate)(implicit messages: Messages): Form[LocalDate] = {
    Form(
      "value" -> planStartDate(
        invalidKey               = "planStartDate.error.invalid",
        allRequiredKey           = "planStartDate.error.required.all",
        twoRequiredKey           = "planStartDate.error.required.two",
        requiredKey              = "planStartDate.error.required",
        beforeEarliestDateKey    = "planStartDate.error.beforeEarliestDate",
        budgetAfterMaxDateKey    = "planStartDate.error.budgetAfterMaxDate",
        timeToPayAfterMaxDateKey = "planStartDate.error.timeToPayAfterMaxDate",
        dateFormats              = DateFormats.defaultDateFormats,
        userAnswers              = userAnswers,
        earliestPlanStartDate    = earliestPlanStartDate
      ).verifying(
        checkIfDateAfter(userAnswers.get(PlanEndDatePage), "planStartDate.error.AfterOrEqualEndDate")
      )
    )
  }

  private def checkIfDateAfter(endDateOpt: Option[LocalDate], errorKey: String): Constraint[LocalDate] =
    Constraint { startDate =>
      endDateOpt match {
        case Some(endDate) if startDate.isAfter(endDate) => Invalid(ValidationError(errorKey))
        case _                                           => Valid
      }
    }

}
