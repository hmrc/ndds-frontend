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

import base.SpecBase

import java.time.LocalDate

class PaymentDateDetailsSpec extends SpecBase {

  val testLocalDate: LocalDate = LocalDate.of(2025, 5, 5)
  val testEarliestPaymentDate: String = "2025-5-10"

  "PaymentDateDetails" - {
    "toPaymentDateDetails method" - {
      "must successfully convert from LocalDate to PaymentDateDetails" in {
        val expectedResult = PaymentDateDetails(testLocalDate, testEarliestPaymentDate)

        val result = PaymentDateDetails.toPaymentDateDetails(testLocalDate, testEarliestPaymentDate)

        result mustEqual expectedResult
      }
    }

    "toLocalDate method" - {
      "must successfully convert from PaymentDateDetails to LocalDate" in {
        val testModel = PaymentDateDetails(testLocalDate, testEarliestPaymentDate)

        val expectedResult = testLocalDate

        val result = PaymentDateDetails.toLocalDate(testModel)

        result mustEqual expectedResult
      }
    }
  }

}
