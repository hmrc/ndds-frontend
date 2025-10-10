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
import models.{DirectDebitSource, PaymentPlanType, UserAnswers}
import pages.{AmendPaymentAmountPage, AmendPlanStartDatePage, BankDetailsAddressPage, BankDetailsBankNamePage, TotalAmountDuePage, YourBankDetailsPage}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}

import java.time.LocalDate

object Utils {
  val emptyString = ""
  val LockExpirySessionKey = "lockoutExpiryDateTime"

  val listHodServices: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT   -> "COTA",
    DirectDebitSource.PAYE -> "PAYE",
    DirectDebitSource.SA   -> "CESA",
    DirectDebitSource.TC   -> "NTC",
    DirectDebitSource.VAT  -> "VAT",
    DirectDebitSource.MGD  -> "MGD",
    DirectDebitSource.NIC  -> "NIDN",
    DirectDebitSource.OL   -> "SAFE",
    DirectDebitSource.SDLT -> "SDLT"
  )

  def generateMacFromAnswers(
    answers: UserAnswers,
    macGenerator: MacGenerator,
    bacsNumber: String
  ): Option[String] = {
    val maybeBankAddress = answers.get(BankDetailsAddressPage)
    val maybeBankName = answers.get(BankDetailsBankNamePage)
    val maybeBankDetails = answers.get(YourBankDetailsPage)

    (maybeBankAddress, maybeBankName, maybeBankDetails) match {
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

    val paymentAmount: BigDecimal = userAnswers
      .get(AmendPaymentAmountPage)
      .orElse(
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentAmount)
      )
      .getOrElse(throw new RuntimeException("Missing Payment Amount"))

    val startDate: LocalDate = planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        userAnswers
          .get(AmendPlanStartDatePage)
          .orElse(userAnswers.get(PaymentPlanDetailsQuery).flatMap(_.paymentPlanDetails.scheduledPaymentStartDate))
          .getOrElse(throw new RuntimeException("Missing start date from both NewAmendPlanStartDatePage and AmendPlanStartDatePage"))

      case PaymentPlanType.BudgetPaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentStartDate)
          .getOrElse(throw new RuntimeException("Missing scheduledPaymentStartDate in PaymentPlanDetailsQuery"))

      case _ =>
        throw new RuntimeException(s"Unsupported plan type: $planType")
    }

    PaymentPlanDuplicateCheckRequest(
      // TODO: Temp data with be replaced with actual data
      directDebitReference = userAnswers.get(DirectDebitReferenceQuery).getOrElse(throw new RuntimeException("Missing DirectDebitReferenceQuery")),
      paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(throw new RuntimeException("Missing PaymentPlanReferenceQuery")),
      planType =
        userAnswers.get(PaymentPlanDetailsQuery).map(_.paymentPlanDetails.planType).getOrElse(throw new RuntimeException("Missing PaymentPlanType")),
      paymentService = userAnswers
        .get(PaymentPlanDetailsQuery)
        .map(_.paymentPlanDetails.hodService)
        .getOrElse(throw new RuntimeException("Missing paymentService")),
      paymentReference = userAnswers
        .get(PaymentPlanDetailsQuery)
        .map(_.paymentPlanDetails.paymentReference)
        .getOrElse(throw new RuntimeException("Missing PaymentPlanDetailsQuery or paymentReference")),
      paymentAmount = paymentAmount,
      totalLiability = userAnswers
        .get(PaymentPlanDetailsQuery)
        .flatMap(_.paymentPlanDetails.balancingPaymentAmount)
        .getOrElse(throw new RuntimeException("Missing TotalLiability")),
      paymentFrequency = 1,
      paymentStartDate = startDate
    )
  }
}
