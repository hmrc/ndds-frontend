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

package pages

import models.{PaymentPlanType, UserAnswers}
import play.api.libs.json.JsPath

import scala.util.Try

case object PaymentPlanTypePage extends QuestionPage[PaymentPlanType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "paymentPlanType"

  override def cleanup(value: Option[PaymentPlanType], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(PaymentPlanType.VariablePaymentPlan) =>
        userAnswers
          .remove(PaymentReferencePage)
          .flatMap(_.remove(PaymentAmountPage))
          .flatMap(_.remove(YearEndAndMonthPage))
          .flatMap(_.remove(PaymentsFrequencyPage))
          .flatMap(_.remove(TotalAmountDuePage))
          .flatMap(_.remove(PaymentDatePage))
      case _ =>
        Try(userAnswers)
    }
  }

}
