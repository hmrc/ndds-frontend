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
import forms.RemovingThisSuspensionFormProvider
import javax.inject.Inject
import models.Mode
import models.responses.PaymentPlanResponse
import navigation.Navigator
import pages.RemovingThisSuspensionPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RemovingThisSuspensionView
import services.NationalDirectDebitService

import scala.concurrent.{ExecutionContext, Future}

class RemovingThisSuspensionController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemovingThisSuspensionFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemovingThisSuspensionView,
  nddsService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    if (nddsService.suspendPaymentPlanGuard(request.userAnswers)) {

      val maybeResult = for {
        planDetails <- request.userAnswers.get(PaymentPlanDetailsQuery)
      } yield {
        val planDetail = planDetails.paymentPlanDetails
        val preparedForm = request.userAnswers.get(RemovingThisSuspensionPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }

        val paymentReference = planDetail.paymentReference
        val suspensionStartDate = planDetail.suspensionStartDate
        val suspensionEndDate = planDetail.suspensionEndDate

        Ok(view(preparedForm, mode, paymentReference, suspensionStartDate, suspensionEndDate))
      }

      maybeResult match {
        case Some(result) => result
        case _            => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    } else {
      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(planDetails) =>
          val errorMessage =
            s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${planDetails.paymentPlanDetails.planType}"
          logger.error(errorMessage)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        case _ =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    if (nddsService.suspendPaymentPlanGuard(request.userAnswers)) {

      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(planDetails) =>
          val planDetail = planDetails.paymentPlanDetails
          val paymentReference = planDetail.paymentReference
          val suspensionStartDate = planDetail.suspensionStartDate
          val suspensionEndDate = planDetail.suspensionEndDate

          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, paymentReference, suspensionStartDate, suspensionEndDate))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemovingThisSuspensionPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(RemovingThisSuspensionPage, mode, updatedAnswers))
            )
        case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    } else {
      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(planDetails) =>
          val errorMessage =
            s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${planDetails.paymentPlanDetails.planType}"
          logger.error(errorMessage)
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }
}
