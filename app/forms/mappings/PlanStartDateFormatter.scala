/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.mappings

import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import pages.{DirectDebitSourcePage, PaymentPlanTypePage}
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlanStartDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  beforeEarliestDateKey: String,
  budgetAfterMaxDateKey: String,
  timeToPayAfterMaxDateKey: String,
  args: Seq[String] = Seq.empty,
  dateFormats: Seq[DateFormat],
  userAnswers: UserAnswers,
  earliestPlanStartDate: LocalDate
)(implicit messages: Messages)
    extends CustomDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args, dateFormats) {

  private val BudgetPaymentPlanMaxYears = 1
  private val TimeToPayMaxDays = 30

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private def fmt(d: LocalDate): String = d.format(formatter)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    super.bind(key, data).flatMap { date =>
      validateBusinessRules(key, date)
    }

  private def validateBusinessRules(key: String, enteredDate: LocalDate): Either[Seq[FormError], LocalDate] = {
    val currentDate = LocalDate.now()
    val errors = scala.collection.mutable.ListBuffer[FormError]()

    if (enteredDate.isBefore(earliestPlanStartDate)) {
      errors += FormError(key, beforeEarliestDateKey, Seq(fmt(earliestPlanStartDate)))
    }

    val paymentPlanType = userAnswers.get(PaymentPlanTypePage)
    val directDebitSource = userAnswers.get(DirectDebitSourcePage)

    if (isBudgetPaymentPlan(paymentPlanType, directDebitSource)) {
      val maxPlanStartDate = currentDate.plusYears(BudgetPaymentPlanMaxYears)
      if (enteredDate.isAfter(maxPlanStartDate)) {
        errors += FormError(key, budgetAfterMaxDateKey, Seq(fmt(maxPlanStartDate)))
      }
    }

    if (isTimeToPayOrVppPaymentPlan(paymentPlanType, directDebitSource)) {
      val maxPlanStartDate = currentDate.plusDays(TimeToPayMaxDays)
      if (enteredDate.isAfter(maxPlanStartDate)) {
        errors += FormError(key, timeToPayAfterMaxDateKey, Seq(fmt(maxPlanStartDate)))
      }
    }

    if (errors.nonEmpty) {
      Left(errors.toSeq)
    } else {
      Right(enteredDate)
    }
  }

  private def isBudgetPaymentPlan(paymentPlanType: Option[PaymentPlanType], directDebitSource: Option[DirectDebitSource]): Boolean = {
    paymentPlanType.contains(PaymentPlanType.BudgetPaymentPlan) && directDebitSource.contains(DirectDebitSource.SA)
  }

  private def isTimeToPayOrVppPaymentPlan(paymentPlanType: Option[PaymentPlanType], directDebitSource: Option[DirectDebitSource]): Boolean = {
    (paymentPlanType.contains(PaymentPlanType.VariablePaymentPlan) && directDebitSource.contains(DirectDebitSource.MGD)) ||
    (paymentPlanType.contains(PaymentPlanType.TaxCreditRepaymentPlan) && directDebitSource.contains(DirectDebitSource.TC))
  }
}
