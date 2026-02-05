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

package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.{Calendar, Date}

class UtilsSpec extends AnyWordSpec with Matchers {

  "getSpecifiedCalendar" should {

    "return a calendar with time set to midnight when date is provided" in {
      // fixed date with non-zero time
      val date = new Date()

      val calendar = Utils.getSpecifiedCalendar(date)

      calendar must not be null
      calendar.get(Calendar.MINUTE) mustBe 0
      calendar.get(Calendar.SECOND) mustBe 0
      calendar.get(Calendar.MILLISECOND) mustBe 0

      // date part should remain the same
      calendar.getTime.toInstant.toEpochMilli / (24 * 60 * 60 * 1000) mustBe
        date.toInstant.toEpochMilli / (24 * 60 * 60 * 1000)
    }

    "return null when date is null" in {
      val calendar = Utils.getSpecifiedCalendar(null)

      calendar mustBe null
    }
  }

}
