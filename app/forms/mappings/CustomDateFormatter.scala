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
        val cleanedValueOpt = data.get(s"$key.$field").map(_.replaceAll("\\s", ""))
        field -> cleanedValueOpt.filter(_.nonEmpty)
      }.toMap

    val missingFields: Seq[String] =
      fieldKeys.filter(f => fields.getOrElse(f, None).isEmpty)

    val regexErrors = dateFormats.flatMap(checkInput(key, fields, _))
    if (regexErrors.nonEmpty) {
      val fieldErrors =
        regexErrors.map(e => e.copy(messages = Seq(invalidKey), args = args))
      val groupError =
        FormError(
          key,
          invalidKey,
          args
        )
      return Left(fieldErrors :+ groupError)
    }

    if (missingFields.nonEmpty) {

      val fieldErrors = missingFields.map { field =>
        FormError(
          s"$key.$field",
          requiredKey,
          Seq(labelFor(field))
        )
      }

      val groupError = missingFields.size match {

        case 1 =>
          FormError(
            key,
            requiredKey,
            Seq(labelFor(missingFields.head))
          )

        case 2 =>
          FormError(
            key,
            twoRequiredKey,
            missingFields.map(labelFor)
          )

        case 3 =>
          FormError(
            key,
            allRequiredKey
          )
      }

      return Left(fieldErrors :+ groupError)
    }

    val cleanedData: Map[String, String] =
      fields.collect { case (k, Some(v)) => s"$key.$k" -> v }

    // 🔹 Normalise month if alphabetic
    val normalisedData: Map[String, String] =
      cleanedData.map {
        case (k, v) if k.endsWith(".month") =>
          val normalisedMonth =
            if (v.matches("""\d{1,2}""")) {
              v
            } else {
              parseMonthName(v).getOrElse(v) // fallback to original if invalid
            }

          k -> normalisedMonth

        case other => other
      }

    formatDate(key, normalisedData).left.map { errors =>
      errors.map(e => e.copy(messages = Seq(invalidKey), args = args))
    }
  }

  private def labelFor(part: String)(implicit messages: Messages): String =
    messages(s"datePart.$part")

  private def checkInput(key: String, fields: Map[String, Option[String]], dateFormat: DateFormat): Option[FormError] = {
    fields.get(dateFormat.dateType).flatten match {
      case Some(dateType) if !dateType.matches(dateFormat.regex) =>
        Some(FormError(s"$key.${dateFormat.dateType}", dateFormat.errorKey, args))
      case _ =>
        None
    }
  }

  private def parseMonthName(input: String): Option[String] = {
    val normalised = input.trim.toLowerCase

    val months = Map(
      "january"   -> 1,
      "jan"       -> 1,
      "february"  -> 2,
      "feb"       -> 2,
      "march"     -> 3,
      "mar"       -> 3,
      "april"     -> 4,
      "apr"       -> 4,
      "may"       -> 5,
      "june"      -> 6,
      "jun"       -> 6,
      "july"      -> 7,
      "jul"       -> 7,
      "august"    -> 8,
      "aug"       -> 8,
      "september" -> 9,
      "sep"       -> 9,
      "october"   -> 10,
      "oct"       -> 10,
      "november"  -> 11,
      "nov"       -> 11,
      "december"  -> 12,
      "dec"       -> 12
    )

    months.get(normalised).map(_.toString)
  }

}
