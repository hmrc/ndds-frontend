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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesImpl}

import java.time.LocalDate

class CustomDateFormatterSpec extends AnyWordSpec with Matchers {

  implicit val messages: Messages = MessagesImpl(Lang("en"), new play.api.i18n.DefaultMessagesApi())

  val dateFormats = Seq(
    DateFormat("day", "error.day", """\d{1,2}"""),
    DateFormat("month", "error.month", """\d{1,2}"""),
    DateFormat("year", "error.year", """\d{4}""")
  )

  val formatter = new CustomDateFormatter(
    invalidKey     = "error.invalid",
    allRequiredKey = "error.allRequired",
    twoRequiredKey = "error.twoRequired",
    requiredKey    = "error.required",
    dateFormats    = dateFormats
  )

  "CustomDateFormatter" should {

    "bind successfully with valid date input" in {
      val data = Map(
        "date.day"   -> "15",
        "date.month" -> "8",
        "date.year"  -> "2025"
      )

      val result = formatter.bind("date", data)

      result mustBe Right(LocalDate.of(2025, 8, 15))
    }

    "return error when some fields are missing" in {
      val data = Map(
        "date.month" -> "8",
        "date.year"  -> "2025"
      )

      val result = formatter.bind("date", data)

      result match {
        case Left(errors) =>
          errors must contain(FormError("date", "error.twoRequired", Seq()))
        case Right(_) =>
          fail("Expected Left with errors but got Right")
      }
    }

    "return error for invalid month format" in {
      val data = Map(
        "date.day"   -> "15",
        "date.month" -> "abc",
        "date.year"  -> "2025"
      )

      val result = formatter.bind("date", data)

      result match {
        case Left(errors) =>
          errors must contain(FormError("date", "error.invalid", Seq()))
        case Right(_) =>
          fail("Expected Left with errors but got Right")
      }
    }

  }
}
