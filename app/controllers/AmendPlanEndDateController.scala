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
import forms.AmendPlanEndDateFormProvider
import models.Mode
import navigation.Navigator
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Frequency
import views.html.AmendPlanEndDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendPlanEndDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanEndDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPlanEndDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val form = formProvider()
    val preparedForm = request.userAnswers.get(AmendPlanEndDatePage) match {
      case None        => form
      case Some(value) => form.fill(Some(value))
    }

    Ok(view(preparedForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val form = formProvider()
    val userAnswers = request.userAnswers

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))),
        maybeEndDate =>
          maybeEndDate match {
            // ✅ CASE 1: User left end date blank
            case None =>
              for {
                updatedAnswers <- Future.fromTry(userAnswers.remove(AmendPlanEndDatePage))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, updatedAnswers))

            // ✅ CASE 2: User entered a date (previous logic)
            case Some(value) =>
              if (nddsService.amendPaymentPlanGuard(userAnswers)) {
                (userAnswers.get(PaymentPlanDetailsQuery), userAnswers.get(AmendPaymentAmountPage)) match {
                  case (Some(planDetails), Some(amendedAmount)) =>
                    val dbAmount = planDetails.paymentPlanDetails.scheduledPaymentAmount.get
                    val dbStartDate = planDetails.paymentPlanDetails.scheduledPaymentStartDate.get
                    val frequencyStr = planDetails.paymentPlanDetails.scheduledPaymentFrequency.getOrElse("MONTHLY")
                    val frequency = Frequency.fromString(frequencyStr)

                    val hasDateChanged = planDetails.paymentPlanDetails.scheduledPaymentEndDate match {
                      case Some(dbEndDate) => value != dbEndDate
                      case _               => true
                    }

                    val hasAmountChanged = amendedAmount != dbAmount
                    val isNoChange = !hasAmountChanged && !hasDateChanged

                    if (isNoChange) {
                      val key = "amendment.noChange"
                      val errorForm = form.fill(Some(value)).withError("value", key)
                      Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                    } else if (hasDateChanged && !hasAmountChanged) {
                      nddsService.calculateNextPaymentDate(dbStartDate, Some(value), frequency).flatMap { result =>
                        if (!result.nextPaymentDateValid) {
                          val errorForm = form.fill(Some(value)).withError("value", "amendPlanEndDate.error.nextPaymentDateValid")
                          Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                        } else {
                          for {
                            updatedAnswers1 <- Future.fromTry(userAnswers.set(AmendPlanEndDatePage, value))
                            updatedAnswers2 <- result.potentialNextPaymentDate.fold(Future.successful(updatedAnswers1)) { nextDate =>
                                                 Future.fromTry(updatedAnswers1.set(AmendPlanStartDatePage, nextDate))
                                               }
                            _ <- sessionRepository.set(updatedAnswers2)
                          } yield Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, updatedAnswers2))

                        }
                      }
                    } else if (!hasDateChanged && hasAmountChanged) {
                      for {
                        duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(userAnswers)
                      } yield {
                        if (duplicateCheckResponse.isDuplicate)
                          Redirect(routes.DuplicateWarningController.onPageLoad(mode).url)
                        else
                          Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, userAnswers))
                      }
                    } else {
                      nddsService.calculateNextPaymentDate(dbStartDate, Some(value), frequency).flatMap { result =>
                        if (!result.nextPaymentDateValid) {
                          val errorForm = form.fill(Some(value)).withError("value", "amendPlanEndDate.error.nextPaymentDateValid")
                          Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                        } else {
                          for {
                            duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(userAnswers)
                            updatedAnswers1        <- Future.fromTry(userAnswers.set(AmendPlanEndDatePage, value))
                            updatedAnswers2 <- result.potentialNextPaymentDate.fold(Future.successful(updatedAnswers1)) { nextDate =>
                                                 Future.fromTry(updatedAnswers1.set(AmendPlanStartDatePage, nextDate))
                                               }
                            _ <- sessionRepository.set(updatedAnswers2)
                          } yield {
                            val logMsg = s"Duplicate check response is ${duplicateCheckResponse.isDuplicate}"
                            if (duplicateCheckResponse.isDuplicate) {
                              logger.warn(logMsg)
                              Redirect(routes.DuplicateWarningController.onPageLoad(mode).url)
                            } else {
                              logger.info(logMsg)
                              Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, updatedAnswers2))
                            }
                          }

                        }
                      }
                    }

                  case _ =>
                    logger.error("Missing Amend payment amount and/or amend plan end date")
                    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }
              } else {
                throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${userAnswers.get(ManagePaymentPlanTypePage).get}")
              }
          }
      )
  }

}
