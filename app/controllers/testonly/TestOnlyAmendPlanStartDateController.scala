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

package controllers.testonly

import controllers.actions.*
import controllers.{JourneyRecoveryController, routes}
import forms.AmendPlanStartDateFormProvider
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.{AmendPaymentAmountPage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.testonly.TestOnlyAmendPlanStartDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPlanStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanStartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPlanStartDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    {
      val answers = request.userAnswers

      if (nddsService.amendPaymentPlanGuard(answers)) {
        val form = formProvider()
        val preparedForm = request.userAnswers
          .get(AmendPlanStartDatePage)
          .orElse(request.userAnswers.get(AmendPlanStartDatePage))
          .fold(form)(form.fill)

        Ok(view(preparedForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
      } else {
        val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }

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
                  checkForDuplicate(mode, userAnswers, value)
                }

              case _ =>
                logger.warn("Missing Amend payment amount and/or amend plan start date")
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
          } else {
            val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
            throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

  private def checkForDuplicate(mode: Mode, userAnswers: UserAnswers, value: LocalDate)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: Request[?]
  ): Future[Result] = {
    for {
      updatedAnswers         <- Future.fromTry(userAnswers.set(AmendPlanStartDatePage, value))
      _                      <- sessionRepository.set(updatedAnswers)
      duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(updatedAnswers)
    } yield {
      logger.warn(s"Duplicate check response is ${duplicateCheckResponse.isDuplicate}")
      if (duplicateCheckResponse.isDuplicate) {
        Redirect(routes.DuplicateWarningController.onPageLoad(mode).url)
      } else {
        Redirect(controllers.testonly.routes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad().url)
      }
    }
  }
}
