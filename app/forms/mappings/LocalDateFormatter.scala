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
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.{LocalDate, Month}
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {

  protected val fieldKeys: List[String] = List("day", "month", "year")

  import java.time.{LocalDate, YearMonth}
  import scala.util.{Failure, Success, Try}

  def isValidDate(year: Int, month: Int, day: Int): Boolean = {
    if (month < 1 || month > 12) return false
    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    day >= 1 && day <= maxDay
  }

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] = {
    if (!isValidDate(year, month, day)) {
      Left(Seq(FormError(key, invalidKey, args)))
    } else {
      Right(LocalDate.of(year, month, day))
    }
  }

  protected def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val int = intFormatter(
      requiredKey    = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey  = invalidKey,
      args
    )

    val month = new MonthFormatter(invalidKey, args)

    for {
      day   <- int.bind(s"$key.day", data)
      month <- month.bind(s"$key.month", data)
      year  <- int.bind(s"$key.year", data)
      date  <- toDate(key, day, month, year)
    } yield date
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields: Map[String, Option[String]] = fieldKeys.map { field =>
      val cleaned = data.get(s"$key.$field").map(_.replaceAll("\\s", ""))
      field -> cleaned.filter(_.nonEmpty)
    }.toMap

    val missingFields: Seq[String] = fields.filter(_._2.isEmpty).keys.toSeq

    missingFields.size match {
      case 0 =>
        val cleanedData = fields.collect { case (k, Some(v)) => s"$key.$k" -> v }
        formatDate(key, cleanedData).left.map { errors =>
          errors
        }

      case 1 | 2 =>
        val fieldErrors = missingFields.map { f =>
          FormError(s"$key.$f", requiredKey, Seq(messages(s"date.error.$f")) ++ args)
        }

        val summaryError = FormError(
          key,
          if (missingFields.size == 1) requiredKey else twoRequiredKey,
          missingFields.map(f => messages(s"date.error.$f")) ++ args
        )

        Left(fieldErrors :+ summaryError)

      case 3 =>
        val fieldErrors = fieldKeys.map { f =>
          FormError(s"$key.$f", requiredKey, Seq(messages(s"date.error.$f")) ++ args)
        }
        val summaryError = FormError(key, allRequiredKey, args)
        Left(fieldErrors :+ summaryError)
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}

private class MonthFormatter(invalidKey: String, args: Seq[String] = Seq.empty) extends Formatter[Int] with Formatters {

  private val baseFormatter = stringFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {
    val months = Month.values.toList
    baseFormatter.bind(key, data).flatMap { str =>
      months
        .find(m =>
          m.getValue.toString == str.replaceAll("^0+", "") ||
            m.toString == str.toUpperCase ||
            m.toString.take(3) == str.toUpperCase
        )
        .map(x => Right(x.getValue))
        .getOrElse(Left(List(FormError(key, invalidKey, args)))) // attach error to month field
    }
  }

  override def unbind(key: String, value: Int): Map[String, String] =
    Map(key -> value.toString)
}
