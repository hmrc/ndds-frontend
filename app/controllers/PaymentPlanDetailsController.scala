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
import scala.concurrent.{Await, ExecutionContext, Future}

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
          val directDebit = response.directDebitDetails
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanTypeQuery, planDetail.planType))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountPage, planDetail.scheduledPaymentAmount))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, planDetail.scheduledPaymentStartDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, planDetail.scheduledPaymentEndDate))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(PaymentReferencePage, planDetail.paymentReference))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(TotalAmountDuePage, planDetail.totalLiability.getOrElse(BigDecimal(0))))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(RegularPaymentAmountPage, planDetail.scheduledPaymentAmount))
            cachedAnswers <- Future.fromTry(updatedAnswers.set(
              YourBankDetailsPage,
              YourBankDetailsWithAuddisStatus(
                accountHolderName = directDebit.bankAccountName,
                sortCode = directDebit.bankSortCode,
                accountNumber = directDebit.bankAccountNumber,
                auddisStatus = directDebit.auddisFlag,
                accountVerified = false
              ) ))
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

}
