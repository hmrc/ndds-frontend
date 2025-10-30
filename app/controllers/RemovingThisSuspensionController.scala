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
import forms.RemovingThisSuspensionFormProvider
import models.requests.ChrisSubmissionRequest

import javax.inject.Inject
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import models.responses.{DirectDebitDetails, PaymentPlanResponse}
import navigation.Navigator
import pages.{RemovingThisSuspensionPage, SuspensionPeriodRangeDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemovingThisSuspensionView
import services.NationalDirectDebitService

import scala.concurrent.{ExecutionContext, Future}

class RemovingThisSuspensionController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemovingThisSuspensionFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemovingThisSuspensionView,
  nddsService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    if (nddsService.suspendPaymentPlanGuard(request.userAnswers)) {

      val maybeResult = for {
        planDetails <- request.userAnswers.get(PaymentPlanDetailsQuery)
      } yield {
        val planDetail = planDetails.paymentPlanDetails
        val preparedForm = request.userAnswers.get(RemovingThisSuspensionPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }

        val paymentReference = planDetail.paymentReference
        val suspensionStartDate = planDetail.suspensionStartDate
        val suspensionEndDate = planDetail.suspensionEndDate

        Ok(view(preparedForm, mode, paymentReference, suspensionStartDate, suspensionEndDate))
      }

      maybeResult match {
        case Some(result) => result
        case _            => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    } else {
      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(planDetails) =>
          val errorMessage =
            s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${planDetails.paymentPlanDetails.planType}"
          logger.error(errorMessage)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        case _ =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      if (nddsService.suspendPaymentPlanGuard(request.userAnswers)) {

        request.userAnswers.get(PaymentPlanDetailsQuery) match {
          case Some(planDetails) =>
            val planDetail = planDetails.paymentPlanDetails
            val paymentReference = planDetail.paymentReference
            val suspensionStartDate = planDetail.suspensionStartDate
            val suspensionEndDate = planDetail.suspensionEndDate

            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(view(formWithErrors, mode, paymentReference, suspensionStartDate, suspensionEndDate))
                  ),
                value =>
                  request.userAnswers.get(queries.DirectDebitReferenceQuery) match {
                    case Some(ddiReference) =>
                      val chrisRequest = removeSuspensionChrisSubmissionRequest(request.userAnswers, ddiReference)

                      // Step 1: Submit CHRIS request first
                      nddsService.submitChrisData(chrisRequest).flatMap { success =>
                        if (success) {
                          logger.info(s"CHRIS submission successful for removing suspension [DDI Ref: $ddiReference]")

                          // Step 2: Only proceed if CHRIS succeeded
                          for {
                            updatedAnswers <- Future.fromTry(request.userAnswers.set(RemovingThisSuspensionPage, value))
                            _              <- sessionRepository.set(updatedAnswers)
                          } yield Redirect(navigator.nextPage(RemovingThisSuspensionPage, mode, updatedAnswers))

                        } else {
                          logger.error(s"CHRIS submission failed for removing suspension [DDI Ref: $ddiReference]")
                          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
                        }
                      }

                    case None =>
                      logger.error("Missing DirectDebitReference in UserAnswers when trying to remove suspension")
                      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
                  }
              )

          case _ =>
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }

      } else {
        request.userAnswers.get(PaymentPlanDetailsQuery) match {
          case Some(planDetails) =>
            val errorMessage =
              s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${planDetails.paymentPlanDetails.planType}"
            logger.error(errorMessage)
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

          case _ =>
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  private def removeSuspensionChrisSubmissionRequest(
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
          removeSuspensionPlan            = true
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

}
