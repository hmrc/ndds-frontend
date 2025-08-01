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

import java.time.LocalDate
import play.api.i18n.Messages

case class DateFormat(dateType: String, errorKey: String, regex: String)


class CustomDateFormatter(invalidKey: String,
                          allRequiredKey: String,
                          twoRequiredKey: String,
                          requiredKey: String,
                          args: Seq[String] = Seq.empty,
                          dateFormats: Seq[DateFormat]
                         )(implicit messages: Messages) extends LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args) {

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val fields = fieldKeys.map {
      field =>
        field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap
    
    lazy val missingFieldErrors = fields.collect {
      case (field, None) =>
        FormError(s"$key.$field", s"date.error.$field")
    }.toList

    val regexErrors = dateFormats.flatMap(checkInput(key, fields, _))

    if (regexErrors.nonEmpty) {
      Left(regexErrors)
    } else if (missingFieldErrors.nonEmpty) {
      Left(missingFieldErrors)
    } else {
      formatDate(key, data).left.map {
        _.map(_.copy(key = key, args = args))
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
