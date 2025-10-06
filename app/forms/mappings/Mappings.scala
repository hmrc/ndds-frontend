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

package forms.mappings

import models.{Enumerable, YearEndAndMonth}
import play.api.data.{FieldMapping, Mapping}
import play.api.data.Forms.{of, optional}
import play.api.i18n.Messages
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", args: Seq[String] = Seq.empty): FieldMapping[String] =
    of(stringFormatter(errorKey, args))

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric",
                    args: Seq[String] = Seq.empty
                   ): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean",
                        args: Seq[String] = Seq.empty
                       ): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))

  protected def enumerable[A](requiredKey: String = "error.required", invalidKey: String = "error.invalid", args: Seq[String] = Seq.empty)(implicit
    ev: Enumerable[A]
  ): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def localDate(invalidKey: String, allRequiredKey: String, twoRequiredKey: String, requiredKey: String, args: Seq[String] = Seq.empty)(
    implicit messages: Messages
  ): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args))

  protected def currency(requiredKey: String = "error.required",
                         invalidNumeric: String = "error.invalidNumeric",
                         nonNumericKey: String = "error.nonNumeric",
                         args: Seq[String] = Seq.empty
                        ): FieldMapping[BigDecimal] =
    of(currencyFormatter(requiredKey, invalidNumeric, nonNumericKey, args))

  protected def customPaymentDate(invalidKey: String,
                                  allRequiredKey: String,
                                  twoRequiredKey: String,
                                  requiredKey: String,
                                  args: Seq[String] = Seq.empty,
                                  dateFormats: Seq[DateFormat]
                                 )(implicit messages: Messages): FieldMapping[LocalDate] =
    of(new CustomDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args, dateFormats))

  protected def planStartDate(invalidKey: String,
                              allRequiredKey: String,
                              twoRequiredKey: String,
                              requiredKey: String,
                              beforeEarliestDateKey: String,
                              budgetAfterMaxDateKey: String,
                              timeToPayAfterMaxDateKey: String,
                              args: Seq[String] = Seq.empty,
                              dateFormats: Seq[DateFormat],
                              userAnswers: models.UserAnswers,
                              earliestPlanStartDate: java.time.LocalDate
                             )(implicit messages: Messages): FieldMapping[java.time.LocalDate] =
    of(
      new PlanStartDateFormatter(
        invalidKey,
        allRequiredKey,
        twoRequiredKey,
        requiredKey,
        beforeEarliestDateKey,
        budgetAfterMaxDateKey,
        timeToPayAfterMaxDateKey,
        args,
        dateFormats,
        userAnswers,
        earliestPlanStartDate
      )
    )

  protected def yearEndMonthDate(invalidKey: String,
                                 allRequiredKey: String,
                                 twoRequiredKey: String,
                                 requiredKey: String,
                                 args: Seq[String] = Seq.empty,
                                 dateFormats: Seq[DateFormat]
                                ): FieldMapping[YearEndAndMonth] =
    of(new YearEndAndMonthDateFormatter(invalidKey, args, dateFormats))

  private def currencyConstraint(
    nonNumericKey: String,
    invalidNumericKey: String
  ): Constraint[String] = Constraint("currencyConstraint") { str =>
    if (!str.matches("""^-?\d+(\.\d{2})?$""")) {
      if (str.exists(ch => !ch.isDigit && ch != '.' && ch != '-')) Invalid(nonNumericKey)
      else Invalid(invalidNumericKey)
    } else {
      Valid
    }
  }

  def currencyWithTwoDecimalsOrWholeNumber(
    requiredKey: String,
    invalidNumericKey: String,
    nonNumericKey: String
  ): Mapping[BigDecimal] =
    text(requiredKey)
      .verifying(currencyConstraint(nonNumericKey, invalidNumericKey))
      .transform[BigDecimal](
        str => BigDecimal(str).setScale(2, BigDecimal.RoundingMode.UNNECESSARY),
        bigDecimal => bigDecimal.setScale(2).underlying.toPlainString
      )

  def optionalLocalDate(
    invalidKey: String,
    allRequiredKey: String,
    twoRequiredKey: String,
    requiredKey: String
  )(implicit messages: Messages): Mapping[Option[LocalDate]] = {
    optional(
      localDate(
        invalidKey     = invalidKey,
        allRequiredKey = allRequiredKey,
        twoRequiredKey = twoRequiredKey,
        requiredKey    = requiredKey
      )
    )
  }
}
