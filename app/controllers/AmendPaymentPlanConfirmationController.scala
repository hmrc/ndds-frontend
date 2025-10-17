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

package controllers

import controllers.actions.*
import models.requests.ChrisSubmissionRequest
import models.responses.{DirectDebitDetails, PaymentPlanDetails}
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AmendPaymentPlanConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPaymentPlanConfirmationView,
  nddService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers

    userAnswers.get(PaymentPlanDetailsQuery) match {
      case Some(response) =>
        val planDetail = response.paymentPlanDetails
        val directDebitDetails = response.directDebitDetails

        val (rows, backLink) = buildRows(userAnswers, planDetail, mode)

        for {
          directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
          paymentPlanReference <- Future.fromTry(Try(userAnswers.get(PaymentPlanReferenceQuery).get))
          planType             <- Future.fromTry(Try(userAnswers.get(AmendPaymentPlanTypePage).get))
        } yield {
          Ok(
            view(
              mode,
              paymentPlanReference,
              directDebitReference,
              directDebitDetails.bankSortCode.getOrElse(""),
              directDebitDetails.bankAccountNumber.getOrElse(""),
              rows,
              backLink
            )
          )
        }

      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val ua = request.userAnswers

      ua.get(DirectDebitReferenceQuery) match {
        case Some(ddiReference) =>
          val chrisRequest = buildChrisSubmissionRequest(ua, ddiReference)
          nddService.submitChrisData(chrisRequest).flatMap { success =>
            if (success) {
              logger.info(s"CHRIS submission successful for DDI Ref [$ddiReference]")
              for {
                directDebitReference <- Future.fromTry(Try(ua.get(DirectDebitReferenceQuery).get))
                paymentPlanReference <- Future.fromTry(Try(ua.get(PaymentPlanReferenceQuery).get))
                lockResponse         <- nddService.lockPaymentPlan(directDebitReference, paymentPlanReference)
              } yield {
                if (lockResponse.lockSuccessful) {
                  logger.info(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
                  println(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
                } else {
                  logger.error(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
                  println(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
                }
                Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad())
              }
            } else {
              logger.error(s"CHRIS submission failed for DDI Ref [$ddiReference]")
              Future.successful(
                Redirect(routes.JourneyRecoveryController.onPageLoad())
                  .flashing("error" -> "There was a problem submitting your direct debit. Please try again later.")
              )
            }
          }

        case None =>
          logger.error("Missing DirectDebitReference in UserAnswers")
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
          amendPlan                       = true
        )

      case None =>
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

  private def buildRows(userAnswers: UserAnswers, paymentPlan: PaymentPlanDetails, mode: Mode)(implicit
    messages: Messages
  ): (Seq[SummaryListRow], Call) =
    userAnswers.get(AmendPaymentPlanTypePage) match {
      case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
        (Seq(
           AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
           AmendPaymentPlanSourceSummary.row(paymentPlan.hodService),
           TotalAmountDueSummary.row(paymentPlan.totalLiability),
           MonthlyPaymentAmountSummary.row(paymentPlan.scheduledPaymentAmount, paymentPlan.totalLiability),
           FinalPaymentAmountSummary.row(paymentPlan.balancingPaymentAmount, paymentPlan.totalLiability),
           PaymentsFrequencySummary.row(paymentPlan.scheduledPaymentFrequency),
           AmendPlanStartDateSummary.row(
             PaymentPlanType.BudgetPaymentPlan.toString,
             userAnswers.get(AmendPlanStartDatePage),
             Constants.shortDateTimeFormatPattern
           ),
           AmendPaymentAmountSummary.row(
             PaymentPlanType.BudgetPaymentPlan.toString,
             userAnswers.get(AmendPaymentAmountPage),
             true
           ),
           AmendPlanEndDateSummary.row(
             userAnswers.get(AmendPlanEndDatePage),
             Constants.shortDateTimeFormatPattern,
             true
           )
         ),
         routes.AmendPlanEndDateController.onPageLoad(mode)
        )

      case _ =>
        (Seq(
           AmendPaymentPlanTypeSummary.row(userAnswers.get(AmendPaymentPlanTypePage).getOrElse("")),
           AmendPaymentPlanSourceSummary.row(paymentPlan.hodService),
           DateSetupSummary.row(paymentPlan.submissionDateTime),
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
         routes.AmendPlanStartDateController.onPageLoad(mode)
        )
    }

}
