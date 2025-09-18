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
import services.NationalDirectDebitService
import java.time.LocalDate

object Utils {
  val emptyString = ""
  val LockExpirySessionKey = "lockoutExpiryDateTime"

  def isTwoDaysPriorPaymentDate(paymentDate: LocalDate, nddService: NationalDirectDebitService, userAnswers: UserAnswers): Boolean =
    val currentDate = LocalDate.now()
    val isSinglePlan = nddService.isSinglePaymentPlan(userAnswers)
    
    if (isSinglePlan) {
      if (paymentDate.isBefore(currentDate.plusDays(3))) true else false
    } else {
      false
    }

  def amendmentGuardPaymentPlan(nddService : NationalDirectDebitService, userAnswers : UserAnswers): Boolean =
    if (nddService.isSinglePaymentPlan(userAnswers) || nddService.isBudgetPaymentPlan(userAnswers)) true else false

  def isThreeDaysPriorPlanEndDate(planEndDate: LocalDate, nddService: NationalDirectDebitService, userAnswers: UserAnswers): Boolean =
    val currentDate = LocalDate.now()
    val isBudgetPlan = nddService.isBudgetPaymentPlan(userAnswers)

    if (isBudgetPlan) {
      if (planEndDate.isBefore(currentDate.plusDays(4))) true else false
    } else {
      false
    }
}

