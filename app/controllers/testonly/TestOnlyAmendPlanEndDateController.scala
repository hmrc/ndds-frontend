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
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import forms.AmendPlanEndDateFormProvider
import models.Mode
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Frequency
import views.html.testonly.TestOnlyAmendPlanEndDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPlanEndDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanEndDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPlanEndDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val answers = request.userAnswers

    if (nddsService.isBudgetPaymentPlan(answers)) {
      val form = formProvider()
      val preparedForm = request.userAnswers.get(AmendPlanEndDatePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()))
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Redirect(routes.SystemErrorController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val form = formProvider()
    val userAnswers = request.userAnswers

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()))),
        value =>
          userAnswers.get(PaymentPlanDetailsQuery) match {
            case Some(planDetails) =>
              val dbStartDate = planDetails.paymentPlanDetails.scheduledPaymentStartDate.get
              val frequencyStr = planDetails.paymentPlanDetails.scheduledPaymentFrequency.getOrElse("MONTHLY")
              val frequency = Frequency.fromString(frequencyStr)

              // F9 check
              if (value.isBefore(dbStartDate)) {
                val errorForm = form
                  .fill(value)
                  .withError(
                    key     = "value",
                    message = "testOnlyAmendPlanEndDate.error.planEndDateBeforeStartDate"
                  )
                Future.successful(
                  BadRequest(view(errorForm, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()))
                )
              } else {
                // F20 check
                nddsService.calculateNextPaymentDate(dbStartDate, Some(value), frequency).flatMap { paymentValidationResult =>
                  if (!paymentValidationResult.nextPaymentDateValid) {
                    val errorForm = form
                      .fill(value)
                      .withError(
                        key     = "value",
                        message = "testOnlyAmendPlanEndDate.error.nextPaymentDateValid"
                      )
                    Future.successful(BadRequest(view(errorForm, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad())))
                  } else {
                    for {
                      updatedAnswers <- Future.fromTry(userAnswers.set(AmendPlanEndDatePage, value))
                      updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDateFlag, true))
                      updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentAmountFlag, false))
                      updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendConfirmRemovePlanEndDateFlag, false))
                      updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPaymentDateFlag, false))
                      updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendRegularPaymentAmountFlag, false))
                      _              <- sessionRepository.set(updatedAnswers)
                    } yield Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad())
                  }
                }
              }

            case _ =>
              logger.warn("Missing Amend payment amount and/or amend plan end date")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }

}
