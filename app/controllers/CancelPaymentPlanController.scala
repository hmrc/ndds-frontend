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
import models.requests.ChrisSubmissionRequest
import models.responses.DirectDebitDetails

import javax.inject.Inject
import models.{DirectDebitSource, NormalMode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, CancelPaymentPlanPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CancelPaymentPlanView

import scala.concurrent.{ExecutionContext, Future}

class CancelPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CancelPaymentPlanFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: CancelPaymentPlanView,
  nddService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
      case (Some(paymentPlanDetail), Some(paymentPlanReference)) =>
        val paymentPlan = paymentPlanDetail.paymentPlanDetails
        val preparedForm = request.userAnswers.get(CancelPaymentPlanPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get))

      case _ =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
            case (Some(paymentPlanDetail), Some(paymentPlanReference)) =>
              val paymentPlan = paymentPlanDetail.paymentPlanDetails
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
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
        },
        value => {
          val ua = request.userAnswers

          ua.get(DirectDebitReferenceQuery) match {
            case Some(ddiReference) =>
              val chrisRequest = buildCancelChrisRequest(ua, ddiReference)
              nddService.submitChrisData(chrisRequest).flatMap { success =>
                if (success) {
                  logger.info(s"CHRIS Cancel submission successful for DDI Ref [$ddiReference]")

                  for {
                    updatedAnswers <- Future.fromTry(ua.set(CancelPaymentPlanPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(routes.CancelPaymentPlanController.onPageLoad()) // cancel confirmation

                } else {
                  logger.error(s"CHRIS submission failed for DDI Ref [$ddiReference]")
                  Future.successful(
                    Redirect(routes.JourneyRecoveryController.onPageLoad())
                      .flashing("error" -> "There was a problem submitting cancel your direct debit plan. Please try again later.")
                  )
                }
              }
            case None =>
              logger.error("Missing DirectDebitReference in UserAnswers")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
        }
      )
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
