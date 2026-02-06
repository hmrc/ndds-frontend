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

package utils

import models.requests.PaymentPlanDuplicateCheckRequest
import models.{DirectDebitSource, PaymentPlanType, PaymentsFrequency, UserAnswers}
import pages.*
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery, Settable}

import java.time.LocalDate
import java.util.{Calendar, Date}

object Utils {
  val emptyString = ""

  private def invertMap[K, V](m: Map[K, V]): Map[V, K] = m map ((k, v) => (v, k))

  val debitSourceToHodMapping: Map[String, String] = Map(
    DirectDebitSource.CT.toString   -> "COTA",
    DirectDebitSource.NIC.toString  -> "NIDN",
    DirectDebitSource.OL.toString   -> "SAFE",
    DirectDebitSource.PAYE.toString -> "PAYE",
    DirectDebitSource.SA.toString   -> "CESA",
    DirectDebitSource.SDLT.toString -> "SDLT",
    DirectDebitSource.TC.toString   -> "NTC",
    DirectDebitSource.VAT.toString  -> "VAT",
    DirectDebitSource.MGD.toString  -> "MGD"
  )
  val hodToDebitSourceMapping: Map[String, String] = invertMap(debitSourceToHodMapping)

  val numericToPlanTypeMapping: Map[String, String] = Map(
    "01" -> PaymentPlanType.SinglePaymentPlan.toString,
    "02" -> PaymentPlanType.BudgetPaymentPlan.toString,
    "03" -> PaymentPlanType.TaxCreditRepaymentPlan.toString,
    "04" -> PaymentPlanType.VariablePaymentPlan.toString
  )
  val planTypeToNumericMapping: Map[String, String] = invertMap(numericToPlanTypeMapping)

  val numericToPaymentFrequencyMapping: Map[Int, String] = Map(
    1 -> PaymentsFrequency.FortNightly.toString,
    2 -> PaymentsFrequency.Weekly.toString,
    3 -> PaymentsFrequency.FourWeekly.toString,
    5 -> PaymentsFrequency.Monthly.toString,
    6 -> PaymentsFrequency.Quarterly.toString,
    7 -> PaymentsFrequency.SixMonthly.toString,
    9 -> PaymentsFrequency.Annually.toString
  )
  val paymentFrequencyToNumericMapping: Map[String, Int] = invertMap(numericToPaymentFrequencyMapping)

  def cleanConfirmationFlags(userAnswers: UserAnswers)(additionalPagesToRemove: Seq[Settable[_]] = Seq()): UserAnswers = {
    val pagesToRemove: Seq[Settable[_]] = Seq(
      AmendPaymentPlanConfirmationPage,
      CreateConfirmationPage,
      SuspensionDetailsCheckYourAnswerPage,
      RemovingThisSuspensionPage,
      CancelPaymentPlanConfirmationPage,
      RemovingThisSuspensionConfirmationPage
    ) :++ additionalPagesToRemove

    pagesToRemove
      .foldLeft(userAnswers) { case (answers, page) =>
        answers.remove(page).getOrElse(userAnswers)
      }
  }

  def generateMacFromAnswers(
    answers: UserAnswers,
    macGenerator: MacGenerator,
    bacsNumber: String
  ): Option[String] = {
    (
      answers.get(BankDetailsAddressPage),
      answers.get(BankDetailsBankNamePage),
      answers.get(YourBankDetailsPage)
    ) match {
      case (Some(bankAddress), Some(bankName), Some(details)) =>
        Some(
          macGenerator.generateMac(
            accountName   = details.accountHolderName,
            accountNumber = details.accountNumber,
            sortCode      = details.sortCode,
            lines         = bankAddress.lines,
            town          = bankAddress.town,
            postcode      = bankAddress.postCode,
            bankName      = bankName,
            bacsNumber    = bacsNumber
          )
        )
      case _ =>
        None
    }
  }

  def buildPaymentPlanCheckRequest(
    userAnswers: UserAnswers,
    directDebitRef: String
  ): PaymentPlanDuplicateCheckRequest = {

    val planType =
      userAnswers.get(PaymentPlanDetailsQuery).map(_.paymentPlanDetails.planType).getOrElse(throw new RuntimeException("Missing PaymentPlanType"))

    val planTypeMapping: Map[String, String] = Map(
      PaymentPlanType.SinglePaymentPlan.toString      -> "01",
      PaymentPlanType.BudgetPaymentPlan.toString      -> "02",
      PaymentPlanType.TaxCreditRepaymentPlan.toString -> "03",
      PaymentPlanType.VariablePaymentPlan.toString    -> "04"
    )

    val paymentFrequencyMapping: Map[String, Int] = Map(
      PaymentsFrequency.FortNightly.toString -> 1,
      PaymentsFrequency.Weekly.toString      -> 2,
      PaymentsFrequency.FourWeekly.toString  -> 3,
      PaymentsFrequency.Monthly.toString     -> 5,
      PaymentsFrequency.Quarterly.toString   -> 6,
      PaymentsFrequency.SixMonthly.toString  -> 7,
      PaymentsFrequency.Annually.toString    -> 9
    )

    val mappedPlanType: String = planTypeMapping.getOrElse(
      planType,
      PaymentPlanType.SinglePaymentPlan.toString
    )

    val paymentAmount: Option[BigDecimal] = userAnswers
      .get(AmendPaymentAmountPage)
      .orElse(
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentAmount)
      )

    val totalLiability: Option[BigDecimal] = planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        userAnswers.get(AmendPaymentAmountPage)

      case PaymentPlanType.BudgetPaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.totalLiability)

      case PaymentPlanType.TaxCreditRepaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.totalLiability)

      case PaymentPlanType.VariablePaymentPlan.toString =>
        None

      case other =>
        throw new RuntimeException(s"Unsupported plan type: $other")
    }

    val startDate: Option[LocalDate] = planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        userAnswers
          .get(AmendPlanStartDatePage)
          .orElse(userAnswers.get(PaymentPlanDetailsQuery).flatMap(_.paymentPlanDetails.scheduledPaymentStartDate))

      case PaymentPlanType.BudgetPaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentStartDate)

      case PaymentPlanType.TaxCreditRepaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentStartDate)

      case PaymentPlanType.VariablePaymentPlan.toString =>
        None

      case _ => None
    }

    val frequency = userAnswers
      .get(PaymentPlanDetailsQuery)
      .flatMap(_.paymentPlanDetails.scheduledPaymentFrequency)
      .flatMap(paymentFrequencyMapping.get)

    val hodService = userAnswers
      .get(PaymentPlanDetailsQuery)
      .map(_.paymentPlanDetails.hodService)
      .flatMap(debitSourceToHodMapping.get)
      .getOrElse(throw new RuntimeException("Missing paymentService"))

    val paymentReference = userAnswers
      .get(PaymentPlanDetailsQuery)
      .map(_.paymentPlanDetails.paymentReference)
      .getOrElse(throw new RuntimeException("Missing PaymentPlanDetailsQuery or paymentReference"))

    planType match {

      case PaymentPlanType.SinglePaymentPlan.toString =>
        PaymentPlanDuplicateCheckRequest(
          directDebitReference = directDebitRef,
          paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(""),
          planType             = mappedPlanType,
          paymentService       = hodService,
          paymentReference     = paymentReference,
          paymentAmount        = paymentAmount,
          totalLiability       = None,
          paymentFrequency     = None,
          paymentStartDate     = startDate
        )

      case PaymentPlanType.BudgetPaymentPlan.toString =>
        PaymentPlanDuplicateCheckRequest(
          directDebitReference = directDebitRef,
          paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(""),
          planType             = mappedPlanType,
          paymentService       = hodService,
          paymentReference     = paymentReference,
          paymentAmount        = paymentAmount,
          totalLiability       = totalLiability,
          paymentFrequency     = frequency,
          paymentStartDate     = startDate
        )

      case PaymentPlanType.TaxCreditRepaymentPlan.toString =>
        PaymentPlanDuplicateCheckRequest(
          directDebitReference = directDebitRef,
          paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(""),
          planType             = mappedPlanType,
          paymentService       = hodService,
          paymentReference     = paymentReference,
          paymentAmount        = paymentAmount,
          totalLiability       = totalLiability,
          paymentFrequency     = None,
          paymentStartDate     = startDate
        )

      case PaymentPlanType.VariablePaymentPlan.toString =>
        PaymentPlanDuplicateCheckRequest(
          directDebitReference = directDebitRef,
          paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(""),
          planType             = mappedPlanType,
          paymentService       = hodService,
          paymentReference     = paymentReference,
          paymentAmount        = None,
          totalLiability       = None,
          paymentFrequency     = None,
          paymentStartDate     = None
        )
    }
  }

  def getSpecifiedCalendar(date: Date): Calendar = {
    if (date != null) {
      val c = Calendar.getInstance()
      c.setTime(date)
      c.set(Calendar.HOUR, 0)
      c.set(Calendar.MINUTE, 0)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      c
    } else {
      null
    }
  }

}
