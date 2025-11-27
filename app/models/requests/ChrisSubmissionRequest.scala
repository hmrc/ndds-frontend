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

package models.requests

import config.FrontendAppConfig
import models.*
import models.audits.{AddPaymentPlanAudit, NewDirectDebitAudit}
import models.audits.AuditType
import pages.*
import play.api.libs.json.{Json, OFormat}
import queries.ExistingDirectDebitIdentifierQuery
import utils.PaymentCalculations
import viewmodels.checkAnswers.*

import java.time.LocalDate

case class ChrisSubmissionRequest(
  serviceType: DirectDebitSource,
  paymentPlanType: PaymentPlanType,
  paymentPlanReferenceNumber: Option[String],
  paymentFrequency: Option[String],
  yourBankDetailsWithAuddisStatus: YourBankDetailsWithAuddisStatus,
  planStartDate: Option[PlanStartDateDetails],
  planEndDate: Option[LocalDate],
  paymentDate: Option[PaymentDateDetails],
  yearEndAndMonth: Option[YearEndAndMonth],
  ddiReferenceNo: String,
  paymentReference: String,
  totalAmountDue: Option[BigDecimal],
  paymentAmount: Option[BigDecimal],
  regularPaymentAmount: Option[BigDecimal],
  amendPaymentAmount: Option[BigDecimal],
  calculation: Option[PaymentPlanCalculation],
  suspensionPeriodRangeDate: Option[SuspensionPeriodRange],
  amendPlan: Boolean = false,
  cancelPlan: Boolean = false,
  suspendPlan: Boolean = false,
  removeSuspensionPlan: Boolean = false,
  addPlan: Boolean = false,
  auditType: Option[AuditType] = None,
  bankAccountType: Option[PersonalOrBusinessAccount] = None
)

object ChrisSubmissionRequest {
  implicit val format: OFormat[ChrisSubmissionRequest] = Json.format[ChrisSubmissionRequest]

  def buildChrisSubmissionRequest(
    userAnswers: UserAnswers,
    ddiReference: String,
    userId: String,
    appConfig: FrontendAppConfig
  ): ChrisSubmissionRequest = {
    implicit val ua: UserAnswers = userAnswers
    val calculationOpt: Option[PaymentPlanCalculation] =
      for {
        source <- ua.get(DirectDebitSourcePage) if source == DirectDebitSource.TC
        plan   <- ua.get(PaymentPlanTypePage) if plan == PaymentPlanType.TaxCreditRepaymentPlan
      } yield calculateTaxCreditRepaymentPlan(ua, appConfig)

    val existingDDIOpt: Option[NddDetails] = ua.get(ExistingDirectDebitIdentifierQuery)
    val hasExistingDDI: Boolean = existingDDIOpt.isDefined
    val auditType = if (hasExistingDDI) {
      Some(AddPaymentPlanAudit)
    } else {
      Some(NewDirectDebitAudit)
    }
    val bankDetailsWithAudis = existingDDIOpt match {
      case Some(existingDd) =>
        YourBankDetailsWithAuddisStatus(
          accountHolderName = existingDd.bankAccountName,
          sortCode          = existingDd.bankSortCode,
          accountNumber     = existingDd.bankAccountNumber,
          auddisStatus      = existingDd.auDdisFlag,
          accountVerified   = true
        )
      case _ => required(YourBankDetailsPage)
    }

    ChrisSubmissionRequest(
      serviceType                     = required(DirectDebitSourcePage),
      paymentPlanType                 = ua.get(PaymentPlanTypePage).getOrElse(PaymentPlanType.SinglePaymentPlan),
      paymentPlanReferenceNumber      = None,
      paymentFrequency                = ua.get(PaymentsFrequencyPage).map(_.toString),
      yourBankDetailsWithAuddisStatus = bankDetailsWithAudis,
      planStartDate                   = ua.get(PlanStartDatePage),
      planEndDate                     = ua.get(PlanEndDatePage),
      paymentDate                     = ua.get(PaymentDatePage),
      yearEndAndMonth                 = ua.get(YearEndAndMonthPage),
      ddiReferenceNo                  = ddiReference,
      paymentReference                = required(PaymentReferencePage),
      totalAmountDue                  = ua.get(TotalAmountDuePage),
      paymentAmount                   = ua.get(PaymentAmountPage),
      regularPaymentAmount            = ua.get(RegularPaymentAmountPage),
      amendPaymentAmount              = None,
      calculation                     = calculationOpt,
      suspensionPeriodRangeDate       = None,
      addPlan                         = hasExistingDDI,
      auditType                       = auditType,
      bankAccountType                 = ua.get(PersonalOrBusinessAccountPage)
    )

  }

  private def calculateTaxCreditRepaymentPlan(userAnswers: UserAnswers, appConfig: FrontendAppConfig): PaymentPlanCalculation = {
    implicit val ua: UserAnswers = userAnswers

    val totalAmountDue = required(TotalAmountDuePage)
    val planStartDate = required(PlanStartDatePage)

    val regularPaymentAmount = PaymentCalculations.calculateRegularPaymentAmount(
      totalAmountDueInput   = totalAmountDue,
      totalNumberOfPayments = appConfig.tcTotalNumberOfPayments
    )

    val finalPaymentAmount = PaymentCalculations.calculateFinalPayment(
      totalAmountDue        = totalAmountDue,
      regularPaymentAmount  = BigDecimal(regularPaymentAmount),
      numberOfEqualPayments = appConfig.tcNumberOfEqualPayments
    )

    val secondPaymentDate = PaymentCalculations.calculateSecondPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset  = appConfig.tcMonthsUntilSecondPayment
    )

    val penultimatePaymentDate = PaymentCalculations.calculatePenultimatePaymentDate(
      planStartDate                = planStartDate.enteredDate,
      penultimateInstallmentOffset = appConfig.tcMonthsUntilPenultimatePayment
    )

    val finalPaymentDate = PaymentCalculations.calculateFinalPaymentDate(
      planStartDate = planStartDate.enteredDate,
      monthsOffset  = appConfig.tcMonthsUntilFinalPayment
    )

    PaymentPlanCalculation(
      regularPaymentAmount   = Some(BigDecimal(regularPaymentAmount)),
      finalPaymentAmount     = Some(finalPaymentAmount),
      secondPaymentDate      = Some(secondPaymentDate),
      penultimatePaymentDate = Some(penultimatePaymentDate),
      finalPaymentDate       = Some(finalPaymentDate),
      monthlyPaymentAmount   = MonthlyPaymentAmountSummary.getMonthlyPaymentAmount(userAnswers)
    )
  }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))
}
