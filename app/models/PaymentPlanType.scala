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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait PaymentPlanType

object PaymentPlanType extends Enumerable.Implicits {

  case object SinglePayment extends WithName("singlePayment") with PaymentPlanType
  case object VariablePaymentPlan extends WithName("variablePaymentPlan") with PaymentPlanType
  case object BudgetPaymentPlan extends WithName("budgetPaymentPlan") with PaymentPlanType
  case object TaxCreditRepaymentPlan extends WithName("taxCreditRepaymentPlan") with PaymentPlanType

  val values1: Seq[PaymentPlanType] = Seq(
    SinglePayment, VariablePaymentPlan
  )

  val values2: Seq[PaymentPlanType] = Seq(
    SinglePayment, BudgetPaymentPlan
  )

  val values3: Seq[PaymentPlanType] = Seq(
    SinglePayment, TaxCreditRepaymentPlan
  )

  def options1(implicit messages: Messages): Seq[RadioItem] = values1.zipWithIndex.map {
    case (value1, index) =>
      RadioItem(
        content = Text(messages(s"paymentPlanType.${value1.toString}")),
        value   = Some(value1.toString),
        id      = Some(s"value1_$index")
      )
  }

  def options2(implicit messages: Messages): Seq[RadioItem] = values2.zipWithIndex.map {
    case (value2, index) =>
      RadioItem(
        content = Text(messages(s"paymentPlanType.${value2.toString}")),
        value   = Some(value2.toString),
        id      = Some(s"value2_$index")
      )
  }

  def options3(implicit messages: Messages): Seq[RadioItem] = values3.zipWithIndex.map {
    case (value3, index) =>
      RadioItem(
        content = Text(messages(s"paymentPlanType.${value3.toString}")),
        value   = Some(value3.toString),
        id      = Some(s"value3_$index")
      )
  }

  implicit val enumerable1: Enumerable[PaymentPlanType] =
    Enumerable(values1.map(v => v.toString -> v): _*)
//    Enumerable(values2.map(v => v.toString -> v): _*)
//    Enumerable(values3.map(v => v.toString -> v): _*)
}
