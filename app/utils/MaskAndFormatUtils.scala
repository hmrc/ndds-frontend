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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object MaskAndFormatUtils {

  def maskSortCode(sortCode: String): String =
    if (sortCode.length >= 6)
      sortCode.take(2) + "****"
    else
      "****"

  def maskAccountNumber(accountNumber: String): String =
    if (accountNumber.length >= 3)
      "***" + accountNumber.drop(3)
    else
      "***"


  private val inputFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
  private val gdsFormatter   = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def formatDateToGds(dateStr: String): String = {
    val localDate = LocalDate.parse(dateStr, inputFormatter)
    localDate.format(gdsFormatter)
  }
}
