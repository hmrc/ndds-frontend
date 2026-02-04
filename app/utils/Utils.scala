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
import pages.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}

import java.time.LocalDate
import java.util.{Calendar, Date}
import scala.concurrent.{ExecutionContext, Future}

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

  def cleanConfirmationFlags(userAnswers: UserAnswers)(implicit ec: ExecutionContext): Future[UserAnswers] = {
    val pagesToRemove = Seq(
      AmendPaymentPlanConfirmationPage,
      CreateConfirmationPage,
      SuspensionDetailsCheckYourAnswerPage,
      RemovingThisSuspensionPage,
      CancelPaymentPlanConfirmationPage,
      RemovingThisSuspensionConfirmationPage
    )

    pagesToRemove
      .foldLeft(Future.successful(userAnswers)) { case (accFut, page) =>
        accFut.flatMap(answers => Future.fromTry(answers.remove(page)))
      }
      .recover { case ex: Throwable =>
        userAnswers
      }
  }

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

    val paymentAmount: Option[BigDecimal] = userAnswers
      .get(AmendPaymentAmountPage)
      .orElse(
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.scheduledPaymentAmount)
      )

    val totalLiability: Option[BigDecimal] = planType match {
      case PaymentPlanType.SinglePaymentPlan.toString =>
        userAnswers
          .get(AmendPaymentAmountPage)

      case PaymentPlanType.BudgetPaymentPlan.toString =>
        userAnswers
          .get(PaymentPlanDetailsQuery)
          .flatMap(_.paymentPlanDetails.totalLiability)

      case _ =>
        throw new RuntimeException(s"Unsupported plan type: $planType")
    }

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

    val hodServiceMapping: Map[String, String] = Map(
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

    PaymentPlanDuplicateCheckRequest(
      directDebitReference = userAnswers.get(DirectDebitReferenceQuery).getOrElse(throw new RuntimeException("Missing DirectDebitReferenceQuery")),
      paymentPlanReference = userAnswers.get(PaymentPlanReferenceQuery).getOrElse(throw new RuntimeException("Missing PaymentPlanReferenceQuery")),
      planType = userAnswers
        .get(PaymentPlanDetailsQuery)
        .map(_.paymentPlanDetails.planType)
        .flatMap(planTypeMapping.get)
        .getOrElse(throw new RuntimeException("Missing PaymentPlanType")),
      paymentService = userAnswers
        .get(PaymentPlanDetailsQuery)
        .map(_.paymentPlanDetails.hodService)
        .flatMap(hodServiceMapping.get)
        .getOrElse(throw new RuntimeException("Missing paymentService")),
      paymentReference = userAnswers
        .get(PaymentPlanDetailsQuery)
        .map(_.paymentPlanDetails.paymentReference)
        .getOrElse(throw new RuntimeException("Missing PaymentPlanDetailsQuery or paymentReference")),
      paymentAmount  = paymentAmount,
      totalLiability = totalLiability,
      paymentFrequency = userAnswers
        .get(PaymentPlanDetailsQuery)
        .flatMap(_.paymentPlanDetails.scheduledPaymentFrequency)
        .flatMap(paymentFrequencyMapping.get),
      paymentStartDate = startDate
    )
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
