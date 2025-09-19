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
import models.{PaymentDateDetails, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetailsWithAuddisStatus}
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import pages.*

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils
import views.html.PaymentPlanDetailsView

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaymentPlanDetailsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: PaymentPlanDetailsView,
                                              nddService: NationalDirectDebitService,
                                              sessionRepository: SessionRepository
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(PaymentReferenceQuery) match {
      case Some(paymentReference) =>
        nddService.getPaymentPlanDetails(paymentReference).flatMap { paymentPlanDetails =>

          val samplePaymentPlanResponse: PaymentPlanResponse =
            PaymentPlanResponse(
              directDebitDetails = DirectDebitDetails(
                bankSortCode = "123456",
                bankAccountNumber = "12345678",
                bankAccountName = "John Doe",
                auddisFlag = true
              ),
              paymentPlanDetails = PaymentPlanDetails(
                hodService = paymentPlanDetails.hodService,
                planType = paymentPlanDetails.planType,
                paymentReference = paymentReference,
                submissionDateTime = paymentPlanDetails.submissionDateTime,
                scheduledPaymentAmount = paymentPlanDetails.scheduledPaymentAmount,
                scheduledPaymentStartDate = LocalDateTime.now().minusMonths(7),
                initialPaymentStartDate = LocalDateTime.now().plusDays(1),
                initialPaymentAmount = BigDecimal(50.00),
                scheduledPaymentEndDate = LocalDateTime.now().plusMonths(12),
                scheduledPaymentFrequency = "Monthly",
                suspensionStartDate = None,
                suspensionEndDate = None,
                balancingPaymentAmount = Some(BigDecimal(25.00)),
                balancingPaymentDate = Some(LocalDateTime.now().plusMonths(13)),
                totalLiability = BigDecimal(1825.50),
                paymentPlanEditable = true
              )
            )

          val paymentPlanType: PaymentPlanType =
            PaymentPlanType.enumerable.withName(paymentPlanDetails.planType).get

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanTypePage, paymentPlanType))
            cachedAnswers <- cachePaymentPlanResponse(samplePaymentPlanResponse, updatedAnswers)
            _ <- sessionRepository.set(cachedAnswers)
          } yield {
            val showActions =
              if (Utils.amendmentGuardPaymentPlan(nddService, cachedAnswers)) {
                if (paymentPlanDetails.planType == PaymentPlanType.BudgetPaymentPlan.toString) {
                  val isThreeDayPrior = Utils.isThreeDaysPriorPlanEndDate(paymentPlanDetails.scheduledPaymentEndDate,
                    nddService, cachedAnswers)
                  !isThreeDayPrior
                } else {
                  val isTwoDayPrior = Utils.isTwoDaysPriorPaymentDate(paymentPlanDetails.scheduledPaymentStartDate,
                    nddService, cachedAnswers)
                  !isTwoDayPrior
                }
              } else {
                false
              }

            Ok(view(paymentReference, paymentPlanDetails, showActions))
          }
        }

      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(paymentReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentReferenceQuery, paymentReference))
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.PaymentPlanDetailsController.onPageLoad())
  }

  private def cachePaymentPlanResponse(
                                        response: PaymentPlanResponse,
                                        userAnswers: UserAnswers
                                      ): Future[UserAnswers] = {

    val paymentPlan = response.paymentPlanDetails
    val directDebit = response.directDebitDetails

    val updatedAnswersTry: Try[UserAnswers] =
      for {
        ua1 <- userAnswers.set(PaymentReferencePage, paymentPlan.paymentReference)
        //ua2 <- ua1.set(PaymentPlanTypePage, paymentPlan.planType) // TODO: map planType string dynamically
        ua3 <- ua1.set(TotalAmountDuePage, paymentPlan.totalLiability)
        ua4 <- ua3.set(AmendPaymentAmountPage, paymentPlan.initialPaymentAmount)
        ua5 <- ua4.set(RegularPaymentAmountPage, paymentPlan.scheduledPaymentAmount)
        ua6 <- ua5.set(PlanStartDatePage, PlanStartDateDetails(
          paymentPlan.initialPaymentStartDate.toLocalDate,
          earliestPlanStartDate = paymentPlan.scheduledPaymentStartDate.toLocalDate.toString
        ))
        ua7 <- ua6.set(AmendPlanEndDatePage, paymentPlan.scheduledPaymentEndDate.toLocalDate)
        ua8 <- ua7.set(PaymentDatePage, PaymentDateDetails(
          enteredDate = paymentPlan.scheduledPaymentStartDate.toLocalDate,
          earliestPaymentDate = paymentPlan.scheduledPaymentStartDate.toLocalDate.toString
        ))
        ua9 <- ua8.set(YourBankDetailsPage, YourBankDetailsWithAuddisStatus(
          accountHolderName = directDebit.bankAccountName,
          sortCode = directDebit.bankSortCode,
          accountNumber = directDebit.bankAccountNumber,
          auddisStatus = directDebit.auddisFlag,
          accountVerified = false
        ))
      } yield ua9

    updatedAnswersTry match {
      case scala.util.Success(updated) => sessionRepository.set(updated).map(_ => updated)
      case scala.util.Failure(ex) => Future.failed(ex)
    }
  }
}
