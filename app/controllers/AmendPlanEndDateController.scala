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
      case Some(value) => form.fill(value)
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
        value =>
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

                if (isNoChange) { // F27 - check
                  val key = "amendment.noChange"
                  val errorForm = form.fill(value).withError("value", key)
                  Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                } else if (hasDateChanged && !hasAmountChanged) {
                  // F20 check
                  nddsService.calculateNextPaymentDate(dbStartDate, value, frequency).flatMap { result =>
                    logger.debug(
                      s"""|[AmendPlanEndDateController]
                          |  nextPaymentDateValid: $result.nextPaymentDateValid
                          |  StartDate: $result.dbStartDate
                          |""".stripMargin
                    )
                    if (!result.nextPaymentDateValid) {
                      val errorForm = form
                        .fill(value)
                        .withError(
                          key     = "value", // form field name in AmendPlanEndDateFormProvider
                          message = "amendPlanEndDate.error.nextPaymentDateValid" // exact key from messages file
                        )
                      Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                    } else {
                      for {
                        updatedAnswers1 <- Future.fromTry(userAnswers.set(AmendPlanEndDatePage, value))
                        updatedAnswers2 <- Future.fromTry(
                                             updatedAnswers1.set(AmendPlanStartDatePage, result.potentialNextPaymentDate)
                                           ) // this needed for budgeting amend end date for chris submission
                        _ <- sessionRepository.set(updatedAnswers2)
                      } yield Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, updatedAnswers2))
                    }
                  }
                } else if (!hasDateChanged && hasAmountChanged) {
                  for {
                    duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(userAnswers) // F26 check
                  } yield {
                    val logMsg = s"Duplicate check response is ${duplicateCheckResponse.isDuplicate}"
                    if (duplicateCheckResponse.isDuplicate) {
                      logger.info(logMsg)
                      Redirect(routes.DuplicateWarningController.onPageLoad(mode).url)
                    } else {
                      logger.debug(logMsg)
                      Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, userAnswers))
                    }
                  }
                } else { // hasDateChanged && hasAmountChanged
                  // F20 check
                  nddsService.calculateNextPaymentDate(dbStartDate, value, frequency).flatMap { result =>
                    logger.info(
                      s"""|[AmendPlanEndDateController]
                          |  nextPaymentDateValid: $result.nextPaymentDateValid
                          |  StartDate: $result.dbStartDate
                          |""".stripMargin
                    )
                    if (!result.nextPaymentDateValid) {
                      val errorForm = form
                        .fill(value)
                        .withError(
                          key     = "value", // form field name in AmendPlanEndDateFormProvider
                          message = "amendPlanEndDate.error.nextPaymentDateValid" // exact key from messages file
                        )
                      Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                    } else {
                      for {
                        duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(userAnswers) // F26 check
                        updatedAnswers1        <- Future.fromTry(userAnswers.set(AmendPlanEndDatePage, value))
                        updatedAnswers2 <- Future.fromTry(
                                             updatedAnswers1.set(AmendPlanStartDatePage, result.potentialNextPaymentDate)
                                           ) // this needed for budgeting amend end date for chris submission
                        _ <- sessionRepository.set(updatedAnswers2)
                      } yield {
                        val logMsg = s"Duplicate check response is ${duplicateCheckResponse.isDuplicate}"
                        if (duplicateCheckResponse.isDuplicate) {
                          logger.info(logMsg)
                          Redirect(routes.DuplicateWarningController.onPageLoad(mode).url)
                        } else {
                          logger.debug(logMsg)
                          Redirect(navigator.nextPage(AmendPlanEndDatePage, mode, userAnswers))
                        }
                      }
                    }
                  }
                }

              case _ =>
                logger.warn("Missing Amend payment amount and/or amend plan end date")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          } else {
            val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
            throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

}
