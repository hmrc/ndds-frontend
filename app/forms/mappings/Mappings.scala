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

import models.Enumerable
import play.api.data.{FieldMapping, Mapping}
import play.api.data.Forms.{of, optional}
import play.api.i18n.Messages

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", args: Seq[String] = Seq.empty): FieldMapping[String] =
    of(stringFormatter(errorKey, args))

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric",
                    args: Seq[String] = Seq.empty): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean",
                        args: Seq[String] = Seq.empty): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))


  protected def enumerable[A](requiredKey: String = "error.required",
                              invalidKey: String = "error.invalid",
                              args: Seq[String] = Seq.empty)(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def localDate(
                           invalidKey: String,
                           allRequiredKey: String,
                           twoRequiredKey: String,
                           requiredKey: String,
                           args: Seq[String] = Seq.empty)(implicit messages: Messages): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args))

  protected def currency(requiredKey: String = "error.required",
                         invalidNumeric: String = "error.invalidNumeric",
                         nonNumericKey: String = "error.nonNumeric",
                         args: Seq[String] = Seq.empty): FieldMapping[BigDecimal] =
    of(currencyFormatter(requiredKey, invalidNumeric, nonNumericKey, args))

  protected def customPaymentDate(
                             invalidKey: String,
                             allRequiredKey: String,
                             twoRequiredKey: String,
                             requiredKey: String,
                             args: Seq[String] = Seq.empty,
                             dateFormats:Seq[DateFormat])(implicit messages: Messages): FieldMapping[LocalDate] =
    of(new CustomDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args, dateFormats))

  protected def yearMonthDate(
                             invalidKey: String,
                             allRequiredKey: String,
                             twoRequiredKey: String,
                             requiredKey: String,
                             args: Seq[String] = Seq.empty,
                             dateFormats:Seq[DateFormat])(implicit messages: Messages): FieldMapping[LocalDate] =
    of(new YearMonthFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args, dateFormats))

  def optionalLocalDate(
                         invalidKey:     String,
                         allRequiredKey: String,
                         twoRequiredKey: String,
                         requiredKey:    String
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
