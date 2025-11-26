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

class YearEndAndMonthDateFormatter(
  emptyKey: String,
  invalidFormatKey: String,
  invalidMonthKey: String,
  args: Seq[String] = Seq.empty,
  dateFormats: Seq[DateFormat]
) extends Formatter[YearEndAndMonth] {

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YearEndAndMonth] = {

    val raw = data.get(key).map(_.trim).getOrElse("")

    // 1. Empty input
    if (raw.isEmpty) {
      return Left(Seq(FormError(key, emptyKey)))
    }

    // 2. Must be exactly 4 digits
    if (!raw.matches("""^\d{4}$""")) {
      return Left(Seq(FormError(key, invalidFormatKey)))
    }

    // Extract parts
    val yearPart = raw.substring(0, 2)
    val monthPart = raw.substring(2, 4)

    // 3. Month must be 01–13
    if (!monthPart.matches("""^(0[1-9]|1[0-3])$""")) {
      return Left(Seq(FormError(key, invalidMonthKey)))
    }

    val year = yearPart.toInt
    val month = monthPart.toInt

    Right(YearEndAndMonth(year, month))
  }

  override def unbind(key: String, value: YearEndAndMonth): Map[String, String] =
    Map(key -> f"${value.year}%02d${value.month}%02d")
}
