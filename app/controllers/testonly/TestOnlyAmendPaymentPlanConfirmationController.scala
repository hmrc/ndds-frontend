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

package controllers.testonly

import controllers.actions.*
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import models.*
import models.audits.AmendPaymentPlanAudit
import models.requests.ChrisSubmissionRequest
import models.responses.DirectDebitDetails
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers
import viewmodels.checkAnswers.*
import views.html.testonly.TestOnlyAmendPaymentPlanConfirmationView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPaymentPlanConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPaymentPlanConfirmationView,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    val alreadyConfirmed: Boolean =
      request.userAnswers.get(AmendPaymentPlanConfirmationPage).contains(true)

    if (alreadyConfirmed) {
      logger.warn("Attempt to load Cancel this payment plan confirmation; redirecting to Page Not Found.")
      Future.successful(Redirect(routes.BackSubmissionController.onPageLoad()))
    } else {
      if (nddService.amendPaymentPlanGuard(userAnswers)) {
        val (rows, backLink) = buildRows(userAnswers, mode)

        Future.successful(Ok(view(mode, rows, backLink)))
      } else {
        val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }

    }
  }

  private def buildRows(userAnswers: UserAnswers, mode: Mode)(implicit
    messages: Messages
  ): (Seq[SummaryListRow], Call) = {
    userAnswers.get(ManagePaymentPlanTypePage) match {
      case Some(PaymentPlanType.SinglePaymentPlan.toString) =>
        (Seq(
           AmendPaymentAmountSummary.row(
             PaymentPlanType.SinglePaymentPlan.toString,
             userAnswers.get(AmendPaymentAmountPage),
             true
           ),
           AmendPlanStartDateSummary.row(
             PaymentPlanType.SinglePaymentPlan.toString,
             userAnswers.get(AmendPlanStartDatePage),
             Constants.shortDateTimeFormatPattern,
             true
           )
         ),
         userAnswers.get(AmendPaymentAmountPage) match {
           case Some(_) => testOnlyRoutes.TestOnlyAmendPaymentAmountController.onPageLoad(mode)
           case _       => testOnlyRoutes.TestOnlyAmendPlanStartDateController.onPageLoad(mode)
         }
        )
      case _ => // Budget Payment Plan
        (Seq(
           AmendPaymentAmountSummary.row(
             PaymentPlanType.BudgetPaymentPlan.toString,
             userAnswers.get(AmendPaymentAmountPage),
             true
           ), // TODO - replace with AP1a TestOnly Amend RegularPaymentAmount
           AmendPlanEndDateSummary.row(
             userAnswers.get(AmendPlanEndDatePage),
             Constants.shortDateTimeFormatPattern,
             true
           ) // TODO - replace with AP1c TestOnly AmendPlanEndDate
         ),
         userAnswers.get(AmendPlanEndDatePage) match {
           case Some(_) => routes.AmendPlanEndDateController.onPageLoad(mode) // TODO - replace with TestOnly Amend plan end date controller
           case _ => routes.RegularPaymentAmountController.onPageLoad(mode) // TODO - replace with TestOnly Amend regular payment amount controller
         }
        )
    }

  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val ua = request.userAnswers

      // F26 duplicate check
      nddService.isDuplicatePaymentPlan(ua).flatMap { duplicateResponse =>
        if (duplicateResponse.isDuplicate) {
          Future.successful(Redirect(testOnlyRoutes.TestOnlyDuplicateWarningController.onPageLoad(mode).url))
        } else {
          submitToChris(ua)
        }
      }

    }

  private def submitToChris(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[Result] = {
    (ua.get(DirectDebitReferenceQuery), ua.get(PaymentPlanReferenceQuery)) match {
      case (Some(ddiReference), Some(paymentPlanReference)) =>
        val chrisRequest = buildChrisSubmissionRequest(ua, ddiReference)
        nddService.submitChrisData(chrisRequest).flatMap { success =>
          if (success) {
            logger.info(s"CHRIS submission successful for amend payment plan for DDI Ref: [$ddiReference]")
            for {
              lockResponse   <- nddService.lockPaymentPlan(ddiReference, paymentPlanReference)
              updatedAnswers <- Future.fromTry(ua.set(AmendPaymentPlanConfirmationPage, true))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              logger.debug(s"Amend payment plan lock returned: ${lockResponse.lockSuccessful}")
              Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad())
            }
          } else {
            logger.error(s"CHRIS submission failed amend payment plan for DDI Ref [$ddiReference]")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
        }

      case _ =>
        logger.error("Missing DirectDebitReference and/or PaymentPlanReference in UserAnswers when trying to amend payment plan confirmation")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def buildChrisSubmissionRequest(
    userAnswers: UserAnswers,
    ddiReference: String
  ): ChrisSubmissionRequest = {
    userAnswers.get(PaymentPlanDetailsQuery) match {
      case Some(response) =>
        val planDetail = response.paymentPlanDetails
        val directDebitDetails = response.directDebitDetails
        val serviceType: DirectDebitSource =
          DirectDebitSource.objectMap.getOrElse(planDetail.planType, DirectDebitSource.SA)

        val planStartDateDetails: Option[PlanStartDateDetails] = userAnswers.get(AmendPlanStartDatePage).map { date =>
          PlanStartDateDetails(enteredDate           = date,
                               earliestPlanStartDate = date.toString // you can adjust this if you have a different logic
                              )
        }

        val paymentPlanType: PaymentPlanType =
          PaymentPlanType.values
            .find(_.toString.equalsIgnoreCase(planDetail.planType))
            .getOrElse(PaymentPlanType.BudgetPaymentPlan)

        val bankDetailsWithAuddisStatus: YourBankDetailsWithAuddisStatus = buildBankDetailsWithAuddisStatus(directDebitDetails)

        ChrisSubmissionRequest(
          serviceType                     = serviceType,
          paymentPlanType                 = paymentPlanType,
          paymentFrequency                = planDetail.scheduledPaymentFrequency,
          paymentPlanReferenceNumber      = userAnswers.get(PaymentPlanReferenceQuery),
          yourBankDetailsWithAuddisStatus = bankDetailsWithAuddisStatus,
          planStartDate                   = planStartDateDetails,
          planEndDate                     = userAnswers.get(AmendPlanEndDatePage),
          paymentDate                     = None,
          yearEndAndMonth                 = None,
          ddiReferenceNo                  = ddiReference,
          paymentReference                = planDetail.paymentReference,
          totalAmountDue                  = planDetail.totalLiability,
          paymentAmount                   = None,
          regularPaymentAmount            = None,
          amendPaymentAmount              = userAnswers.get(AmendPaymentAmountPage),
          calculation                     = None,
          suspensionPeriodRangeDate       = None,
          amendPlan                       = true,
          auditType                       = Some(AmendPaymentPlanAudit)
        )

      case _ =>
        throw new IllegalStateException("Missing PaymentPlanDetails in userAnswers")
    }
  }

  private def buildBankDetailsWithAuddisStatus(
    directDebitDetails: DirectDebitDetails
  ): YourBankDetailsWithAuddisStatus = {
    val bankDetails = YourBankDetails(
      accountHolderName = directDebitDetails.bankAccountName.getOrElse(""),
      sortCode = directDebitDetails.bankSortCode.getOrElse(
        throw new IllegalStateException("Missing bank sort code")
      ),
      accountNumber = directDebitDetails.bankAccountNumber.getOrElse(
        throw new IllegalStateException("Missing bank account number")
      )
    )

    YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(
      yourBankDetails = bankDetails,
      auddisStatus    = directDebitDetails.auDdisFlag,
      accountVerified = true
    )
  }

}
