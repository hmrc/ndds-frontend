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
import models.{PaymentDateDetails, PaymentPlanType, PaymentsFrequency, PlanStartDateDetails, UserAnswers, YourBankDetailsWithAuddisStatus}
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import pages.*

import javax.inject.Inject
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanTypeQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils
import views.html.PaymentPlanDetailsView

import java.time.{LocalDate, LocalDateTime}
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
                submissionDateTime = Some(paymentPlanDetails.submissionDateTime),
                scheduledPaymentAmount = Some(paymentPlanDetails.scheduledPaymentAmount),
                scheduledPaymentStartDate =  Some(LocalDateTime.now().minusMonths(7)),
                initialPaymentStartDate =  Some(LocalDateTime.now().plusDays(1)),
                initialPaymentAmount =  Some(BigDecimal(50.00)),
                scheduledPaymentEndDate =  Some(LocalDate.now().plusMonths(12)),
                scheduledPaymentFrequency =  Some(PaymentsFrequency.Weekly),
                  suspensionStartDate = None,
                suspensionEndDate = None,
                balancingPaymentAmount = Some(BigDecimal(25.00)),
                balancingPaymentDate = Some(LocalDateTime.now().plusMonths(13)),
                totalLiability = Some(BigDecimal(1825.50)),
                paymentPlanEditable = true
              )
            )

          val paymentPlanType: PaymentPlanType =
            PaymentPlanType.enumerable.withName(paymentPlanDetails.planType).get

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanTypeQuery, paymentPlanDetails.planType))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, paymentPlanDetails.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, paymentPlanDetails.scheduledPaymentStartDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, paymentPlanDetails.scheduledPaymentEndDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(PaymentPlanTypePage, paymentPlanType))
            cachedAnswers <- cachePaymentPlanResponse(samplePaymentPlanResponse, updatedAnswers)
            _ <- sessionRepository.set(cachedAnswers)
          } yield {
            val showActions =
              if (Utils.amendPaymentPlanGuard(nddService, cachedAnswers)) {
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
    //val paymentPlanType: PaymentPlanType = PaymentPlanType.enumerable.withName(paymentPlan.planType).get

    for {
      updatedAnswer <- Future.fromTry(userAnswers.set(PaymentReferencePage, paymentPlan.paymentReference))
      updatedAnswer <- Future.fromTry(updatedAnswer.set(AmendPaymentPlanTypePage, paymentPlan.planType))
      updatedAnswer <- Future.fromTry(updatedAnswer.set(AmendTotalAmountDuePage, paymentPlan.totalLiability))
      updatedAnswer <- Future.fromTry(updatedAnswer.set(AmendPaymentAmountPage, paymentPlan.scheduledPaymentAmount.getOrElse(0)))
      updatedAnswer <- Future.fromTry(
        updatedAnswer.set(
          YourBankDetailsPage,
          YourBankDetailsWithAuddisStatus(
            accountHolderName = directDebit.bankAccountName,
            sortCode = directDebit.bankSortCode,
            accountNumber = directDebit.bankAccountNumber,
            auddisStatus = directDebit.auddisFlag,
            accountVerified = true
          )
        )
      )
      _ <- sessionRepository.set(updatedAnswer)
    } yield updatedAnswer
  }


}
