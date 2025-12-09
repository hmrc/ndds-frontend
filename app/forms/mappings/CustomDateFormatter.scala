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

    val day = data.get(s"$key.day").filter(_.nonEmpty)
    val month = data.get(s"$key.month").filter(_.nonEmpty)
    val year = data.get(s"$key.year").filter(_.nonEmpty)

    val missing =
      List("day" -> day, "month" -> month, "year" -> year).collect { case (field, None) =>
        field
      }

    missing match {

      case Nil =>

      case List("day", "month", "year") =>
        return Left(
          Seq(FormError(key, allRequiredKey, args))
        )

      case List(a, b) =>
        return Left(
          Seq(FormError(key, twoRequiredKey, Seq(a, b) ++ args))
        )

      case List(field) =>
        return Left(
          Seq(FormError(s"$key.$field", requiredKey, Seq(field) ++ args))
        )
    }

    val fields = Map("day" -> day, "month" -> month, "year" -> year)

    val regexErrors =
      dateFormats.flatMap(df => checkInput(key, fields, df))

    if (regexErrors.nonEmpty)
      return Left(regexErrors)

    formatDate(key, data).left.map(_.map(_.copy(args = args)))
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
