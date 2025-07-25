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
import forms.mappings.DateFormat

class DateFormatSpec extends AnyFreeSpec with Matchers {

  "A DateFormat" - {
    "must construct with correct values" in {
      val df = DateFormat("day", "error.key", "^\\d{1,2}$")
      df.dateType mustEqual "day"
      df.errorKey mustEqual "error.key"
      df.regex mustEqual "^\\d{1,2}$"
    }

    "must match valid day input with regex" in {
      val df = DateFormat("day", "error.key", "^([1-9]|0[1-9]|1[0-9]|2[0-9]|3[0-1])$")
      "1" must fullyMatch regex df.regex
      "09" must fullyMatch regex df.regex
      "31" must fullyMatch regex df.regex
      "00" must not (fullyMatch regex df.regex)
      "32" must not (fullyMatch regex df.regex)
      "abc" must not (fullyMatch regex df.regex)
    }

    "must match valid month input with regex" in {
      val df = DateFormat("month", "error.key", "^(0?[1-9]|1[0-2])$")
      "1" must fullyMatch regex df.regex
      "09" must fullyMatch regex df.regex
      "12" must fullyMatch regex df.regex
      "00" must not (fullyMatch regex df.regex)
      "13" must not (fullyMatch regex df.regex)
      "abc" must not (fullyMatch regex df.regex)
    }

    "must match valid year input with regex" in {
      val df = DateFormat("year", "error.key", "^[0-9]{4}$")
      "2024" must fullyMatch regex df.regex
      "24" must not (fullyMatch regex df.regex)
      "abcd" must not (fullyMatch regex df.regex)
    }
  }
}
