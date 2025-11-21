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

  override def cleanup(value: Option[PaymentPlanType], ua: UserAnswers): Try[UserAnswers] = {
    val previousValue = ua.get(PaymentPlanTypePage)
    (previousValue, value) match {
      case (Some(oldValue), Some(newValue)) if oldValue != newValue =>
        // Radio selection plan type changed → clear dependent pages
        ua.remove(PaymentReferencePage)
          .flatMap(_.remove(PaymentsFrequencyPage))
          .flatMap(_.remove(RegularPaymentAmountPage))
          .flatMap(_.remove(PlanStartDatePage))
          .flatMap(_.remove(AddPaymentPlanEndDatePage))
          .flatMap(_.remove(PlanEndDatePage))
          .flatMap(_.remove(PaymentAmountPage))
          .flatMap(_.remove(PaymentDatePage))
          .flatMap(_.remove(TotalAmountDuePage))
      case _ =>
        // No change → do default behavior
        super.cleanup(value, ua)
    }
  }

}
