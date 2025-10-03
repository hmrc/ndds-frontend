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
import models.{DirectDebitSource, PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.PaymentReferencePage
import queries.{DirectDebitReferenceQuery, PaymentReferenceQuery}

object Utils {
  val emptyString = ""
  val LockExpirySessionKey = "lockoutExpiryDateTime"

  def buildPaymentPlanCheckRequest(
                                    userAnswers: UserAnswers,
                                    directDebitRef: String
                                  ): PaymentPlanDuplicateCheckRequest = {

    PaymentPlanDuplicateCheckRequest(
      //TODO: Temp data with be replaced with actual data
      directDebitReference = userAnswers.get(DirectDebitReferenceQuery).get,
      paymentPlanReference = userAnswers.get(PaymentReferenceQuery).get,
      planType = PaymentPlanType.SinglePaymentPlan.toString,
      paymentService = DirectDebitSource.SA.toString,
      paymentReference = userAnswers.get(PaymentReferencePage).get,
      paymentAmount =  BigDecimal(120.00),
      totalLiability = BigDecimal(780.00),
      paymentFrequency = PaymentsFrequency.Weekly.toString
    )
  }
}

