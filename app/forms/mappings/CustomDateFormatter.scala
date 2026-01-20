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

import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

case class DateFormat(dateType: String, errorKey: String, regex: String)

class CustomDateFormatter(invalidKey: String,
                          allRequiredKey: String,
                          twoRequiredKey: String,
                          requiredKey: String,
                          args: Seq[String] = Seq.empty,
                          dateFormats: Seq[DateFormat]
                         )(implicit messages: Messages)
    extends LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args) {

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields: Map[String, Option[String]] =
      fieldKeys.map { field =>
        field -> data.get(s"$key.$field").map(_.trim).filter(_.nonEmpty)
      }.toMap

    val missing: List[String] =
      fields.collect { case (field, None) => field }.toList
    val missingCount = missing.size

    if (missingCount == fieldKeys.size) {
      Left(Seq(FormError(key, allRequiredKey, args)))
    } else if (missingCount > 0) {
      val messageKey = if (missingCount == 1) requiredKey else twoRequiredKey
      Left(missing.map(field => FormError(s"$key.$field", messageKey, args)))
    } else {
      val regexErrors: Seq[FormError] =
        dateFormats.flatMap(df => checkInput(key, fields, df))

      if (regexErrors.nonEmpty) {
        Left(regexErrors)
      } else {
        formatDate(key, data).left.map(_.map(_.copy(key = key, args = args)))
      }
    }
  }

  private def checkInput(key: String, fields: Map[String, Option[String]], dateFormat: DateFormat): Option[FormError] = {
    fields.get(dateFormat.dateType).flatten match {
      case Some(dateType) if !dateType.matches(dateFormat.regex) =>
        Some(FormError(s"$key.${dateFormat.dateType}", dateFormat.errorKey, args))
      case _ =>
        None
    }
  }
}
