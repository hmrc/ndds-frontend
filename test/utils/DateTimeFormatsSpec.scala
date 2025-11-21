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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Lang
import utils.DateTimeFormats.dateTimeFormat

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateTimeFormatsSpec extends AnyFreeSpec with Matchers {

  ".dateTimeFormat" - {

    "must format dates in English" in {
      val formatter = dateTimeFormat()(Lang("en"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Jan 2023"
    }

    "must format dates in Welsh" in {
      val formatter = dateTimeFormat()(Lang("cy"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Ion 2023"
    }

    "must default to English format" in {
      val formatter = dateTimeFormat()(Lang("de"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Jan 2023"
    }

    "formattedCurrentDate" - {
      "should return today's date in 'd MMMM yyyy' format" in {
        val today = LocalDate.now()
        val expected = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        DateTimeFormats.formattedCurrentDate mustEqual expected
      }
    }

    "formattedDateTimeShort" - {
      "should return today's date in 'dd MMM yyyy' format" in {
        val dateStr = "2021-07-31"
        val expected = "31 Jul 2021"

        DateTimeFormats.formattedDateTimeShort(dateStr) mustEqual expected
      }
    }

    "formattedDateTimeNumeric" - {
      "should return today's date in 'dd MM yyyy' format" in {
        val dateStr = "2021-07-31"
        val expected = "31 07 2021"

        DateTimeFormats.formattedDateTimeNumeric(dateStr) mustEqual expected
      }
    }
  }
}
