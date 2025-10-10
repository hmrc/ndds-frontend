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
import forms.AmendPlanStartDateFormProvider
import models.Mode
import navigation.Navigator
import pages.{AmendPaymentAmountPage, AmendPaymentPlanTypePage, AmendPlanStartDatePage, NewAmendPlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendPlanStartDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendPlanStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanStartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPlanStartDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    {
      val form = formProvider()
      val preparedForm = request.userAnswers
        .get(AmendPlanStartDatePage)
        .orElse(request.userAnswers.get(AmendPlanStartDatePage))
        .fold(form)(form.fill)

      Ok(view(preparedForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
    }
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

                val isNoChange = amendedAmount == dbAmount && value == dbStartDate

                if (isNoChange) {
                  val key = "amendment.noChange"
                  val errorForm = form.fill(value).withError("value", key)
                  Future.successful(BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode))))
                } else {
                  for {
                    updatedAnswers <- if (value != dbStartDate) {
                                        Future.fromTry(userAnswers.set(NewAmendPlanStartDatePage, value))
                                      } else {
                                        Future.fromTry(userAnswers.set(AmendPlanStartDatePage, value))
                                      }
                    duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(updatedAnswers)
                    updatedAnswers         <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, value))
                    _                      <- sessionRepository.set(updatedAnswers)
                  } yield {
                    val isDuplicate = duplicateCheckResponse.isDuplicate
                    if (isDuplicate) {
                      println("Duplicate check response is " + isDuplicate)
                      logger.warn("Duplicate check response is " + isDuplicate)
                      // TODO: Replace with new Warning page DTR-542 DW1
                      Redirect(routes.JourneyRecoveryController.onPageLoad())
                    } else {
                      println("Duplicate check response is " + isDuplicate)
                      logger.info("Duplicate check response is " + isDuplicate)
                      Redirect(navigator.nextPage(AmendPlanStartDatePage, mode, updatedAnswers))
                    }
                  }
                }

              case _ =>
                logger.error("Missing Amend payment amount and/or amend plan start date")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          } else {
            throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${userAnswers.get(AmendPaymentPlanTypePage).get}")
          }
      )
  }
}
