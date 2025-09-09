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
import services.NationalDirectDebitService
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

class UtilsSpec extends AnyFunSuite with MockitoSugar {

  val mockService: NationalDirectDebitService = mock[NationalDirectDebitService]
  val userAnswersId: String = "id"
  val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val expectedUserAnswers: UserAnswers = emptyUserAnswers

  test("return true if it is a budget payment plan") {
    when(mockService.isBudgetPaymentPlan(expectedUserAnswers)).thenReturn(true)
    val result = Utils.amendmentGuardPaymentPlan(mockService, expectedUserAnswers)
    assert(result)
  }

  test("return true if it is a single payment plan") {
    when(mockService.isSinglePaymentPlan(expectedUserAnswers)).thenReturn(true)
    val result = Utils.amendmentGuardPaymentPlan(mockService, expectedUserAnswers)
    assert(result)
  }

  test("return false if it is not a single payment plan or budget payment plan") {
    when(!(mockService.isSinglePaymentPlan(expectedUserAnswers)) &&
      !(mockService.isBudgetPaymentPlan(expectedUserAnswers))).thenReturn(false)
    val result = Utils.amendmentGuardPaymentPlan(mockService, expectedUserAnswers)
    assert(result)
  }
  
}
