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
import models.responses.PaymentPlanDetailsResponse
import pages.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import queries.PaymentReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils
import utils.Utils.listHodServices
import views.html.PaymentPlanDetailsView

import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PaymentPlanDetailsController @Inject()(
                                              val controllerComponents: MessagesControllerComponents,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              sessionRepository: SessionRepository,
                                              nddService: NationalDirectDebitService,
                                              view: PaymentPlanDetailsView
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(PaymentReferenceQuery) match {
        case Some(paymentReference) =>
          nddService.getPaymentPlanDetails(paymentReference).flatMap { response =>


            val plan = response.paymentPlanDetails
            val directDebit = response.directDebitDetails

            val maybeSource: Option[DirectDebitSource] = {
              listHodServices.find { case (_, v) => v.equalsIgnoreCase(plan.hodService) }
                .map(_._1)
            }

            val planForView = PaymentPlanDetailsResponse(
              hodService = plan.hodService,
              planType = plan.planType,
              paymentReference = plan.paymentReference,
              submissionDateTime = plan.submissionDateTime.getOrElse(LocalDateTime.now),
              scheduledPaymentAmount = plan.scheduledPaymentAmount,
              scheduledPaymentStartDate = plan.scheduledPaymentStartDate.getOrElse(LocalDate.now),
              initialPaymentStartDate = plan.initialPaymentStartDate,
              initialPaymentAmount = plan.initialPaymentAmount,
              scheduledPaymentEndDate = plan.scheduledPaymentEndDate.getOrElse(LocalDate.now),
              scheduledPaymentFrequency = plan.scheduledPaymentFrequency.map(_.toString),
              suspensionStartDate = plan.suspensionStartDate,
              suspensionEndDate = plan.suspensionEndDate,
              balancingPaymentAmount = plan.balancingPaymentAmount,
              balancingPaymentDate = plan.balancingPaymentDate,
              totalLiability = plan.totalLiability,
              paymentPlanEditable = plan.paymentPlanEditable
            )
            // Update UserAnswers consistently
            val updatedAnswersTry: Try[UserAnswers] = for {
              updatedAnswer <- request.userAnswers.set(PaymentReferencePage, plan.paymentReference)
              updatedAnswer <- request.userAnswers.set(AmendPaymentPlanSourcePage, maybeSource.getOrElse("").toString)
              updatedAnswer <- updatedAnswer.set(AmendPaymentPlanTypePage, plan.planType)
              updatedAnswer <- updatedAnswer.set(TotalAmountDuePage, plan.totalLiability.getOrElse(BigDecimal(0)))
              updatedAnswer <- updatedAnswer.set(AmendPaymentAmountPage, plan.scheduledPaymentAmount)
              updatedAnswer <- updatedAnswer.set(AmendPlanStartDatePage, plan.scheduledPaymentStartDate.getOrElse(LocalDate.now()))
              updatedAnswer <- updatedAnswer.set(AmendPlanEndDatePage, plan.scheduledPaymentEndDate.getOrElse(LocalDate.now()))
              updatedAnswer <- updatedAnswer.set(RegularPaymentAmountPage, plan.scheduledPaymentAmount)
              updatedAnswer <- updatedAnswer.set(
                YourBankDetailsPage,
                YourBankDetailsWithAuddisStatus(
                  accountHolderName = directDebit.bankAccountName,
                  sortCode = directDebit.bankSortCode,
                  accountNumber = directDebit.bankAccountNumber,
                  auddisStatus = directDebit.auddisFlag,
                  accountVerified = false
                )
              )
            } yield updatedAnswer

            updatedAnswersTry match {
              case Success(updatedAnswers) =>
                sessionRepository.set(updatedAnswers).map { _ =>
                  // Determine showActions based on plan type and utils

                  val showActions = if (Utils.amendPaymentPlanGuard(nddService, updatedAnswers)) {
                    if (plan.planType == PaymentPlanType.BudgetPaymentPlan.toString) {
                      val isThreeDayPrior = Utils.isThreeDaysPriorPlanEndDate(
                        plan.scheduledPaymentEndDate.getOrElse(LocalDate.now),
                        nddService,
                        updatedAnswers
                      )
                      !isThreeDayPrior
                    } else {
                      val isTwoDayPrior = Utils.isTwoDaysPriorPaymentDate(
                        plan.scheduledPaymentStartDate.getOrElse(LocalDate.now),
                        nddService,
                        updatedAnswers
                      )
                      !isTwoDayPrior
                    }
                  } else false

                  Ok(view(paymentReference, planForView, showActions))
                }

              case Failure(ex) =>
                Future.failed(ex)
            }
          }

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onRedirect(paymentReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val updatedAnswersTry: Try[UserAnswers] =
        request.userAnswers.set(PaymentReferenceQuery, paymentReference)

      updatedAnswersTry match {
        case Success(updatedAnswers) =>
          sessionRepository.set(updatedAnswers).map(_ =>
            Redirect(routes.PaymentPlanDetailsController.onPageLoad())
          )
        case Failure(ex) =>
          Future.failed(ex)
      }
    }
}
