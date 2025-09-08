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

import models.UserAnswers
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import services.NationalDirectDebitService
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate

class UtilsSpec extends AnyFunSuite with MockitoSugar {

    val mockService: NationalDirectDebitService = mock[NationalDirectDebitService]
  val userAnswersId: String = "id"
  val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val expectedUserAnswersChangeMode: UserAnswers = emptyUserAnswers.copy(data =
    Json.obj(
      "paymentDate" -> Json.obj(
        "enteredDate" -> "2025-02-01",
        "earliestPaymentDate" -> "2025-02-01",
      )))

    test("return true if payment date is less than a day from current date") {
      when(mockService.isSinglePaymentPlan(expectedUserAnswersChangeMode)).thenReturn(true)

      val result = Utils.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(1), mockService, expectedUserAnswersChangeMode)
      assert(result)
    }

    test("return true if payment date is equal to 2 days from current date") {
      when(mockService.isSinglePaymentPlan(expectedUserAnswersChangeMode)).thenReturn(true)
      val result = Utils.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(2), mockService, expectedUserAnswersChangeMode)
      assert(result)
    }

    test("return false if payment date is more than 2 days from current date") {
      when(mockService.isSinglePaymentPlan(expectedUserAnswersChangeMode)).thenReturn(true)
      val result = Utils.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(3), mockService, expectedUserAnswersChangeMode)
      assert(!result)
    }

    test("return false if not a single payment date") {
      when(mockService.isSinglePaymentPlan(expectedUserAnswersChangeMode)).thenReturn(false)
      val result = Utils.isTwoDaysPriorPaymentDate(LocalDate.now().plusDays(1), mockService, expectedUserAnswersChangeMode)
      assert(!result)
    }
}
