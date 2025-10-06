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

  case object SinglePaymentPlan      extends WithName("singlePaymentPlan") with PaymentPlanType
  case object VariablePaymentPlan    extends WithName("variablePaymentPlan") with PaymentPlanType
  case object BudgetPaymentPlan      extends WithName("budgetPaymentPlan") with PaymentPlanType
  case object TaxCreditRepaymentPlan extends WithName("taxCreditRepaymentPlan") with PaymentPlanType

  // All values used for Enumerable mapping
  val values: Seq[PaymentPlanType] = Seq(
    SinglePaymentPlan,
    VariablePaymentPlan,
    BudgetPaymentPlan,
    TaxCreditRepaymentPlan
  )

  val values1: Seq[PaymentPlanType] = Seq(
    SinglePaymentPlan,
    VariablePaymentPlan
  )

  val values2: Seq[PaymentPlanType] = Seq(
    SinglePaymentPlan,
    BudgetPaymentPlan
  )

  val values3: Seq[PaymentPlanType] = Seq(
    SinglePaymentPlan,
    TaxCreditRepaymentPlan
  )

  def options(values: Seq[PaymentPlanType], prefix: String)(implicit messages: Messages): Seq[RadioItem] =
    values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"paymentPlanType.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"${prefix}_$index")
      )
    }

  def options1(implicit messages: Messages): Seq[RadioItem] = options(values1, "value1")

  def options2(implicit messages: Messages): Seq[RadioItem] = options(values2, "value2")

  def options3(implicit messages: Messages): Seq[RadioItem] = options(values3, "value3")

  implicit val enumerable: Enumerable[PaymentPlanType] =
    Enumerable(values.map(v => v.toString -> v)*)

}
