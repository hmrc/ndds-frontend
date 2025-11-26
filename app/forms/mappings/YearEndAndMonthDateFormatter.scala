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

import scala.util.Try

private[mappings] class YearEndAndMonthDateFormatter(
  invalidKey: String,
  args: Seq[String] = Seq.empty,
  dateFormats: Seq[DateFormat]
) extends Formatter[YearEndAndMonth]
    with Formatters {

  private val yearKey = "year"
  private val monthKey = "month"

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YearEndAndMonth] = {

    // 1. Fetch the raw value from "value"
    val raw = data.get(key).map(_.trim).getOrElse("")

    // 2. Required check
    if (raw.isEmpty) {
      return Left(Seq(FormError(key, invalidKey)))
    }

    // 3. Must be exactly 4 digits
    if (!raw.matches("""^\d{4}$""")) {
      return Left(Seq(FormError(key, invalidKey)))
    }

    // 4. Split into year + month components
    val yearPart = raw.substring(0, 2)
    val monthPart = raw.substring(2, 4)

    if (!monthPart.matches("""^(0[1-9]|1[0-3])$""")) {
      return Left(Seq(FormError(key, invalidKey)))
    }

    // 5. Inject into the data map so the original logic can run
    val expandedData = data ++ Map(
      s"$key.$yearKey"  -> yearPart,
      s"$key.$monthKey" -> monthPart
    )

    // Run the existing validation pipeline (unchanged)
    bindExpanded(key, expandedData)
  }

  private def bindExpanded(key: String, data: Map[String, String]): Either[Seq[FormError], YearEndAndMonth] = {
    val yearField = s"$key.$yearKey"
    val monthField = s"$key.$monthKey"

    val yearOpt = data.get(yearField)
    val monthOpt = data.get(monthField)

    if (yearOpt.isEmpty || monthOpt.isEmpty) {
      return Left(Seq(FormError(key, invalidKey)))
    }

    // Regex validation (your original dateFormats)
    val regexErrors =
      dateFormats.flatMap { format =>
        data.get(s"$key.${format.dateType}") match {
          case Some(value) if !value.matches(format.regex) =>
            Some(FormError(s"$key.${format.dateType}", format.errorKey, args))
          case _ =>
            None
        }
      }

    if (regexErrors.nonEmpty) {
      return Left(regexErrors)
    }

    // Parse values
    val year = yearOpt.get.toInt
    val month = monthOpt.get.toInt

    // Month range check
    if (month < 1 || month > 13) {
      return Left(Seq(FormError(key, invalidKey)))
    }

    Right(YearEndAndMonth(year, month))
  }

  override def unbind(key: String, value: YearEndAndMonth): Map[String, String] =
    Map(key -> f"${value.year}%02d${value.month}%02d")
}
