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
import models.responses.DirectDebitDetails
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.{SuspensionDetailsCheckYourAnswerPage, SuspensionPeriodRangeDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.CheckYourSuspensionDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourSuspensionDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourSuspensionDetailsView,
  nddService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    logger.info("Display suspension details confirmation page")
    val summaryList = buildSummaryList(request.userAnswers)
    Ok(view(summaryList, mode, routes.SuspensionPeriodRangeDateController.onPageLoad(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val confirmed = true

      request.userAnswers.get(queries.DirectDebitReferenceQuery) match {
        case Some(ddiReference) =>
          val chrisRequest = suspendChrisSubmissionRequest(request.userAnswers, ddiReference)

          nddService.submitChrisData(chrisRequest).flatMap { success =>
            if (success) {
              logger.debug(s"CHRIS submission successful for suspend budgeting payment plan for DDI Ref [$ddiReference]")
              for {
                updatedAnswers <- Future.fromTry(
                                    request.userAnswers.set(SuspensionDetailsCheckYourAnswerPage, confirmed)
                                  )
                _ <- sessionRepository.set(updatedAnswers)
              } yield {
                Redirect(navigator.nextPage(SuspensionDetailsCheckYourAnswerPage, mode, updatedAnswers))
              }
            } else {
              logger.error(s"CHRIS submission for suspend budgeting payment plan failed with DDI Ref [$ddiReference]")
              Future.successful(
                Redirect(routes.JourneyRecoveryController.onPageLoad())
              )
            }
          }

        case _ =>
          logger.error("Missing DirectDebitReference in UserAnswers when trying to submit suspension details")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def suspendChrisSubmissionRequest(
    userAnswers: UserAnswers,
    ddiReference: String
  ): ChrisSubmissionRequest = {
    userAnswers.get(PaymentPlanDetailsQuery) match {
      case Some(response) =>
        val planDetail = response.paymentPlanDetails
        val directDebitDetails = response.directDebitDetails
        val serviceType: DirectDebitSource =
          DirectDebitSource.objectMap.getOrElse(planDetail.planType, DirectDebitSource.SA)

        val planStartDateDetails: Option[PlanStartDateDetails] = planDetail.scheduledPaymentStartDate.map { date =>
          PlanStartDateDetails(enteredDate = date, earliestPlanStartDate = date.toString)
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
          planEndDate                     = planDetail.scheduledPaymentEndDate,
          paymentDate                     = None,
          yearEndAndMonth                 = None,
          ddiReferenceNo                  = ddiReference,
          paymentReference                = planDetail.paymentReference,
          totalAmountDue                  = planDetail.totalLiability,
          paymentAmount                   = planDetail.scheduledPaymentAmount,
          regularPaymentAmount            = None,
          amendPaymentAmount              = None,
          suspensionPeriodRangeDate       = userAnswers.get(SuspensionPeriodRangeDatePage),
          calculation                     = None,
          suspendPlan                     = true
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

  private def buildSummaryList(answers: models.UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        SuspensionPeriodRangeDateSummary.row(answers, true)
      ).flatten
    )

}
