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

import models.requests.PaymentPlanDuplicateCheckRequest
import models.{PaymentPlanType, PaymentsFrequency, UserAnswers}

object Utils {
  val emptyString = ""
  val LockExpirySessionKey = "lockoutExpiryDateTime"

  def buildPaymentPlanCheckRequest(
                                    userAnswers: UserAnswers,
                                    directDebitRef: String
                                  ): PaymentPlanDuplicateCheckRequest = {
    implicit val ua: UserAnswers = userAnswers

    PaymentPlanDuplicateCheckRequest(
      //TODO: Temp data with be replaced with actual data
      directDebitReference = directDebitRef,
      paymentPlanReference = "payment ref 123",
      planType = PaymentPlanType.SinglePaymentPlan.toString,
      paymentService = "CESA",
      paymentReference = "payment ref",
      paymentAmount =  120.00,
      totalLiability = 780.00,
      paymentFrequency = "WEEKLY"
    )
  }
}

