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

package utils

import play.api.i18n.{Lang, Messages}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import java.util.Locale

object DateTimeFormats {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

  private val localisedDateTimeFormatters = Map(
    "en" -> dateTimeFormatter,
    "cy" -> dateTimeFormatter.withLocale(new Locale("cy"))
  )

  def dateTimeFormat()(implicit lang: Lang): DateTimeFormatter = {
    localisedDateTimeFormatters.getOrElse(lang.code, dateTimeFormatter)
  }

  def formattedCurrentDate(implicit messages: Messages): String = {
    val formatter = DateTimeFormatter.ofPattern(
      "d MMMM yyyy",
      messages.lang.locale
    )
    LocalDate.now().format(formatter)
  }

  def formattedDateTime(dateTime: ZonedDateTime): String = { // example: 24 July 2020, 16:29pm in local zone BST time (ZonedDateTime)
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mma")
    val extractDateSuffix = """(.*)(am|pm|AM|PM)""".r

    dateTime.format(formatter) match {
      case extractDateSuffix(dateThing, suffix) => dateThing + suffix.toLowerCase
    }
  }

  def formattedDateTimeShort(dateTime: String)(implicit messages: Messages): String = {
    val date = LocalDate.parse(dateTime)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", messages.lang.locale)
    date.format(formatter)
  }

  def formattedDateTimeNumeric(dateTime: String): String = { // example: 17 11 2017
    val date = LocalDate.parse(dateTime)
    val formatter = DateTimeFormatter.ofPattern("dd MM yyyy", Locale.UK)

    date.format(formatter)
  }

  val dateTimeHintFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d M yyyy")

}
