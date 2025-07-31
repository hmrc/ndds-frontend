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

import models.YearEndAndMonth
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import scala.util.Try

private[mappings] class YearEndAndMonthDateFormatter(
                                            invalidKey: String,
                                            allRequiredKey: String,
                                            twoRequiredKey: String,
                                            requiredKey: String,
                                            args: Seq[String] = Seq.empty,
                                            dateFormats: Seq[DateFormat]
                                          )(implicit messages: Messages) extends Formatter[YearEndAndMonth] with Formatters {

  protected val fieldKeys: List[String] = List("year", "month")

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YearEndAndMonth] = {
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

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], YearEndAndMonth] = {
    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    val monthFormatter = stringFormatter(invalidKey, args)

    for {
      year  <- int.bind(s"$key.year", data)
      monthStr <- monthFormatter.bind(s"$key.month", data)
      month <- Try(monthStr.replaceAll("^0+", "").toInt).toEither.left.map(_ => 
        Seq(FormError(s"$key.month", invalidKey, args))
      ).flatMap { monthValue =>
        if (monthValue >= 1 && monthValue <= 13) {
          Right(monthValue)
        } else {
          Left(Seq(FormError(s"$key.month", invalidKey, args)))
        }
      }
    } yield YearEndAndMonth(year, month)
  }

  override def unbind(key: String, value: YearEndAndMonth): Map[String, String] =
    Map(
      s"$key.year" -> value.year.toString,
      s"$key.month" -> value.month.toString
    )
}
