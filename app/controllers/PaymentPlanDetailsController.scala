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
import models.*
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
import views.html.PaymentPlanDetailsView
import scala.concurrent.duration.*
import java.time.LocalDateTime
import scala.concurrent.{Await, ExecutionContext, Future}
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
        nddService.getPaymentPlanDetails(paymentReference).flatMap { response =>

          val planDetail = response.paymentPlanDetails

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanTypeQuery, planDetail.planType))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, planDetail.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, planDetail.scheduledPaymentStartDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, planDetail.scheduledPaymentEndDate))
            cachedAnswers <- cachePaymentPlanResponse(response, updatedAnswers)
            _ <- sessionRepository.set(cachedAnswers)
          } yield {
            val showActions =
              val flag: Future[Boolean] = planDetail.planType match {
                case PaymentPlanType.SinglePaymentPlan.toString =>
                  nddService.isTwoDaysPriorPaymentDate(planDetail.scheduledPaymentStartDate)
                case PaymentPlanType.BudgetPaymentPlan.toString =>
                  for {
                    isTwoDaysBeforeStart <- nddService.isTwoDaysPriorPaymentDate(planDetail.scheduledPaymentStartDate)
                    isThreeDaysBeforeEnd <- nddService.isThreeDaysPriorPlanEndDate(planDetail.scheduledPaymentEndDate)
                  } yield isTwoDaysBeforeStart && isThreeDaysBeforeEnd
                case PaymentPlanType.VariablePaymentPlan.toString => Future.successful(false)
                case _ => Future.successful(false)
              }
              Await.result(flag, 5.seconds)

            val showCancelAction = PaymentPlanType.VariablePaymentPlan.toString == planDetail.planType
            Ok(view(paymentReference, planDetail, showActions, showCancelAction))
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

    val paymentPlanType: PaymentPlanType =
      PaymentPlanType.enumerable.withName(paymentPlan.planType).get

    val updatedAnswersTry: Try[UserAnswers] =
      for {
        ua1 <- userAnswers.set(PaymentReferencePage, paymentPlan.paymentReference)
        ua2 <- ua1.set(PaymentPlanTypePage, paymentPlanType)
        ua3 <- ua2.set(TotalAmountDuePage, paymentPlan.totalLiability.getOrElse(0))
        ua4 <- ua3.set(AmendPaymentAmountPage, paymentPlan.scheduledPaymentAmount)
        ua5 <- ua4.set(RegularPaymentAmountPage, paymentPlan.scheduledPaymentAmount)
        ua6 <- ua5.set(PlanStartDatePage, PlanStartDateDetails(
          paymentPlan.initialPaymentStartDate.toLocalDate,
          earliestPlanStartDate = paymentPlan.scheduledPaymentStartDate.toString
        ))
        ua7 <- ua6.set(AmendPlanEndDatePage, paymentPlan.scheduledPaymentEndDate)
        ua8 <- ua7.set(PaymentDatePage, PaymentDateDetails(
          enteredDate = paymentPlan.scheduledPaymentStartDate,
          earliestPaymentDate = paymentPlan.scheduledPaymentStartDate.toString
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
