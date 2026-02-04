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

package services

import models.*
import models.audits.AmendPaymentPlanAudit
import models.requests.ChrisSubmissionRequest
import models.responses.DirectDebitDetails
import pages.*
import play.api.Logging
import play.api.mvc.Result
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChrisSubmissionForAmendService @Inject() (
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitToChris(ua: UserAnswers, successRedirect: => Result, errorRedirect: => Result)(implicit hc: HeaderCarrier): Future[Result] = {
    (ua.get(DirectDebitReferenceQuery), ua.get(PaymentPlanReferenceQuery)) match {
      case (Some(ddiRef), Some(ppRef)) =>
        val request = buildChrisSubmissionRequest(ua, ddiRef)

        nddService.submitChrisData(request).flatMap { ok =>
          if (ok) {
            for {
              lockResponse <- nddService.lockPaymentPlan(ddiRef, ppRef)
              updatedUa    <- Future.fromTry(ua.set(AmendPaymentPlanConfirmationPage, true))
              _            <- sessionRepository.set(updatedUa)
            } yield successRedirect
          } else {
            logger.error(s"CHRIS submission failed amend payment plan for DDI Ref: $ddiRef")
            Future.successful(errorRedirect)
          }
        }

      case _ =>
        logger.error("Missing DDI ref and/or PaymentPlanReference in UserAnswers during CHRIS submission for amend payment plan")
        Future.successful(errorRedirect)
    }
  }

  /** Builds the submission request */
  private def buildChrisSubmissionRequest(
    ua: UserAnswers,
    ddiRef: String
  ): ChrisSubmissionRequest = {

    ua.get(PaymentPlanDetailsQuery) match {
      case Some(details) =>
        val plan = details.paymentPlanDetails
        val dd = details.directDebitDetails

        val paymentPlanType = PaymentPlanType.values
          .find(_.toString.equalsIgnoreCase(plan.planType))
          .getOrElse(PaymentPlanType.BudgetPaymentPlan)

        val startDate = ua.get(AmendPlanStartDatePage).map { d =>
          PlanStartDateDetails(enteredDate = d, earliestPlanStartDate = d.toString)
        }

        ChrisSubmissionRequest(
          serviceType                     = DirectDebitSource.objectMap.getOrElse(plan.planType, DirectDebitSource.SA),
          paymentPlanType                 = paymentPlanType,
          paymentFrequency                = plan.scheduledPaymentFrequency,
          paymentPlanReferenceNumber      = ua.get(PaymentPlanReferenceQuery),
          yourBankDetailsWithAuddisStatus = buildBankDetailsWithAuddisStatus(dd),
          planStartDate                   = startDate,
          planEndDate                     = ua.get(AmendPlanEndDatePage),
          paymentDate                     = None,
          yearEndAndMonth                 = None,
          ddiReferenceNo                  = ddiRef,
          paymentReference                = plan.paymentReference,
          totalAmountDue                  = plan.totalLiability,
          paymentAmount                   = None,
          regularPaymentAmount            = None,
          amendPaymentAmount              = ua.get(AmendPaymentAmountPage),
          calculation                     = None,
          suspensionPeriodRangeDate       = None,
          amendPlan                       = true,
          auditType                       = Some(AmendPaymentPlanAudit)
        )

      case _ =>
        throw new IllegalStateException("Missing PaymentPlanDetails from UserAnswers")
    }
  }

  private def buildBankDetailsWithAuddisStatus(dd: DirectDebitDetails): YourBankDetailsWithAuddisStatus = {
    val bank = YourBankDetails(
      accountHolderName = dd.bankAccountName.getOrElse(""),
      sortCode          = dd.bankSortCode.getOrElse(throw new IllegalStateException("Missing bank sort code")),
      accountNumber     = dd.bankAccountNumber.getOrElse(throw new IllegalStateException("Missing bank account number"))
    )

    YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(
      bank,
      dd.auDdisFlag,
      accountVerified = true
    )
  }
}
