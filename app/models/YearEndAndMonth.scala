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

package models

import play.api.libs.json.*

import java.time.LocalDate

case class YearEndAndMonth(year: Int, month: Int) {
  def toLocalDate: LocalDate = {
    val actualMonth = if (month == YearEndAndMonth.SpecialMonthValue) YearEndAndMonth.December else month
    LocalDate.of(year, actualMonth, YearEndAndMonth.FirstDayOfMonth)
  }

  def displayFormat: String = {
    f"$year%04d $month%02d"
  }
}

object YearEndAndMonth {
  private val SpecialMonthValue = 13
  private val December = 12
  private val FirstDayOfMonth = 1

  implicit val format: OFormat[YearEndAndMonth] = Json.format[YearEndAndMonth]

  def fromLocalDate(localDate: LocalDate): YearEndAndMonth = {
    YearEndAndMonth(localDate.getYear, localDate.getMonthValue)
  }
}
