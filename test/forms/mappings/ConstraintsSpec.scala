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

import java.time.LocalDate

import config.CurrencyFormatter
import generators.Generators
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.validation.{Invalid, Valid}

class ConstraintsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators  with Constraints {


  "firstError" - {

    "must return Valid when all constraints pass" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("foo")
      result mustEqual Valid
    }

    "must return Invalid when the first constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }

    "must return Invalid when the second constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.regexp", """^\w+$""")
    }

    "must return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength(-1, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result mustEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result mustEqual Invalid("error.max", 1)
    }
  }

  "regexp" - {

    "must return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result mustEqual Valid
    }

    "must return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result mustEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" - {

    "must return Valid for a string shorter than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 9)
      result mustEqual Valid
    }

    "must return Valid for an empty string" in {
      val result = maxLength(10, "error.length")("")
      result mustEqual Valid
    }

    "must return Valid for a string equal to the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 10)
      result mustEqual Valid
    }

    "must return Invalid for a string longer than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }
  }

  "maxLengthWithoutSpaces" - {

    "must return Valid for a string shorter than the allowed length" in {
      val result = maxLengthWithoutSpaces(6, "error.length")(" a " * 5)
      result mustEqual Valid
    }

    "must return Valid for an empty string" in {
      val result = maxLengthWithoutSpaces(6, "error.length")("")
      result mustEqual Valid
    }

    "must return Valid for a string equal to the allowed length" in {
      val result = maxLengthWithoutSpaces(6, "error.length")(" a" * 6)
      result mustEqual Valid
    }

    "must return Invalid for a string longer than the allowed length" in {
      val result = maxLengthWithoutSpaces(6, "error.length")("a " * 8)
      result mustEqual Invalid("error.length", 6)
    }
  }

  "minLengthWithoutSpaces" - {

    "must return Valid for a string equal to or longer than the minimum length" in {
      val result = minLengthWithoutSpaces(3, "error.tooShort")(" abc")
      result mustEqual Valid
    }
    "must return Invalid for a string shorter than the minimum length" in {
      val result = minLengthWithoutSpaces(3, "error.tooShort")(" ab")
      result mustEqual Invalid("error.tooShort", 3)
    }
  }

  "maxDate" - {

    "must return Valid for a date before or equal to the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), max)
      } yield (max, date)

      forAll(gen) {
        case (max, date) =>

          val result = maxDate(max, "error.future")(date)
          result mustEqual Valid
      }
    }

    "must return Invalid for a date after the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(max.plusDays(1), LocalDate.of(3000, 1, 2))
      } yield (max, date)

      forAll(gen) {
        case (max, date) =>

          val result = maxDate(max, "error.future", "foo")(date)
          result mustEqual Invalid("error.future", "foo")
      }
    }
  }

  "minDate" - {

    "must return Valid for a date after or equal to the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(min, LocalDate.of(3000, 1, 1))
      } yield (min, date)

      forAll(gen) {
        case (min, date) =>

          val result = minDate(min, "error.past", "foo")(date)
          result mustEqual Valid
      }
    }

    "must return Invalid for a date before the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(LocalDate.of(2000, 1, 2), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), min.minusDays(1))
      } yield (min, date)

      forAll(gen) {
        case (min, date) =>

          val result = minDate(min, "error.past", "foo")(date)
          result mustEqual Invalid("error.past", "foo")
      }
    }
  }


  "minimumCurrency" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(BigDecimal(1.01))
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumCurrency(1, "error.min").apply(0.99)
      result mustEqual Invalid("error.min", CurrencyFormatter.currencyFormat(1))
    }
  }

  "maximumCurrency" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumCurrency(1, "error.max").apply(1.01)
      result mustEqual Invalid("error.max", CurrencyFormatter.currencyFormat(1))
    }
  }

  "minLength" - {
    "must return Valid for a string equal to or longer than the minimum length" in {
      val result = minLength(3, "error.tooShort")("abc")
      result mustEqual Valid
    }
    "must return Invalid for a string shorter than the minimum length" in {
      val result = minLength(3, "error.tooShort")("ab")
      result mustEqual Invalid("error.tooShort", 3)
    }
  }

  "NumericRegex" - {
    "must match only numeric strings" in {
      "123456" must fullyMatch regex NumericRegex
      "000" must fullyMatch regex NumericRegex
      "12a34" mustNot fullyMatch regex NumericRegex
      "abc" mustNot fullyMatch regex NumericRegex
      "12 34" mustNot fullyMatch regex NumericRegex
      "£$%^" mustNot fullyMatch regex NumericRegex
      "" mustNot fullyMatch regex NumericRegex
    }
  }

  "NumericRegexWithSpaces" - {
    "must match numeric strings with spaces" in {
      "123456" must fullyMatch regex NumericRegexWithSpaces
      "12 34 56" must fullyMatch regex NumericRegexWithSpaces
      "000" must fullyMatch regex NumericRegexWithSpaces
      "12a34" mustNot fullyMatch regex NumericRegexWithSpaces
      "abc" mustNot fullyMatch regex NumericRegexWithSpaces
      "£$%^" mustNot fullyMatch regex NumericRegexWithSpaces
      "" mustNot fullyMatch regex NumericRegexWithSpaces
    }
  }
}