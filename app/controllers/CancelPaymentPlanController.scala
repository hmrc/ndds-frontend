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
import forms.CancelPaymentPlanFormProvider
import models.requests.{ChrisSubmissionRequest, DataRequest}
import models.responses.DirectDebitDetails
import models.{DirectDebitSource, NormalMode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.{CancelPaymentPlanPage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CancelPaymentPlanView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CancelPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddService: NationalDirectDebitService,
  formProvider: CancelPaymentPlanFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: CancelPaymentPlanView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    if (nddService.isPaymentPlanCancellable(request.userAnswers)) {
      (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
        case (Some(paymentPlanDetail), Some(paymentPlanReference)) =>
          val paymentPlan = paymentPlanDetail.paymentPlanDetails
          val preparedForm = request.userAnswers.get(CancelPaymentPlanPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get))

        case _ =>
          logger.warn("Unable to load CancelPaymentPlanController missing PaymentPlanDetailsQuery or PaymentPlanReferenceQuery")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot cancel this plan type: $planType")
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => handleFormErrors(formWithErrors),
        value => handleValidSubmission(value)
      )
  }

  private def handleFormErrors(formWithErrors: Form[?])(implicit request: DataRequest[AnyContent]): Future[Result] = {
    (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
      case (Some(planDetail), Some(paymentPlanReference)) =>
        val paymentPlan = planDetail.paymentPlanDetails
        Future.successful(
          BadRequest(
            view(
              formWithErrors,
              paymentPlan.planType,
              paymentPlanReference,
              paymentPlan.scheduledPaymentAmount.get
            )
          )
        )
      case _ =>
        logger.warn(s"Unable to submit CancelPaymentPlanController missing PaymentPlanDetailsQuery or PaymentPlanReferenceQuery")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def handleValidSubmission(value: Boolean)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val ua = request.userAnswers

    ua.get(DirectDebitReferenceQuery) match {
      case Some(ddiReference) =>
        val chrisRequest = buildCancelChrisRequest(ua, ddiReference)

        nddService.submitChrisData(chrisRequest).flatMap {
          case true =>
            logger.info(s"CHRIS Cancel payment plan payload submission successful for DDI Ref [$ddiReference]")

            for {
              updatedAnswers       <- Future.fromTry(ua.set(CancelPaymentPlanPage, value))
              directDebitReference <- Future.fromTry(Try(ua.get(DirectDebitReferenceQuery).get))
              paymentPlanReference <- Future.fromTry(Try(ua.get(PaymentPlanReferenceQuery).get))
              lockResponse         <- nddService.lockPaymentPlan(directDebitReference, paymentPlanReference)
              _                    <- sessionRepository.set(updatedAnswers)
            } yield {
              if (lockResponse.lockSuccessful) {
                logger.info(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
              } else {
                logger.error(s"Payment plan lock returns: ${lockResponse.lockSuccessful}")
              }
              Redirect(navigator.nextPage(CancelPaymentPlanPage, NormalMode, updatedAnswers))
            }
          case false =>
            logger.error(s"CHRIS Cancel plan submission failed for DDI Ref [$ddiReference]")
            Future.successful(
              Redirect(routes.JourneyRecoveryController.onPageLoad())
            )
        }

      case None =>
        logger.error("Missing DirectDebitReference in UserAnswers")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def buildCancelChrisRequest(
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
          totalAmountDue                  = None,
          paymentAmount                   = planDetail.scheduledPaymentAmount,
          regularPaymentAmount            = None,
          amendPaymentAmount              = None,
          calculation                     = None,
          suspensionPeriodRangeDate       = None,
          cancelPlan                      = true
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
