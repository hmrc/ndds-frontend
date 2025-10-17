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
import forms.CancelPaymentPlanFormProvider

import javax.inject.Inject
import models.NormalMode
import navigation.Navigator
import pages.{CancelPaymentPlanPage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CancelPaymentPlanView

import scala.concurrent.{ExecutionContext, Future}

class CancelPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddService: NationalDirectDebitService,
  formProvider: CancelPaymentPlanFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: CancelPaymentPlanView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    if (nddService.isPaymentPlanCancellable(request.userAnswers)) {
      (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
        case (Some(paymentPlanDetail), Some(paymentPlanReference)) =>
          val paymentPlan = paymentPlanDetail.paymentPlanDetails
          val preparedForm = request.userAnswers.get(CancelPaymentPlanPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, paymentPlan.planType, paymentPlanReference, paymentPlan.scheduledPaymentAmount.get))

        case _ =>
          logger.error("Unable to load CancelPaymentPlanController missing PaymentPlanDetailsQuery or PaymentPlanReferenceQuery")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot cancel this plan type: $planType")
      // TODO removed after testing
      println(s"NDDS Payment Plan Guard: Cannot cancel this plan type: $planType")
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          (request.userAnswers.get(PaymentPlanDetailsQuery), request.userAnswers.get(PaymentPlanReferenceQuery)) match {
            case (Some(paymentPlanDetail), Some(paymentPlanReference)) =>
              val paymentPlan = paymentPlanDetail.paymentPlanDetails
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    paymentPlan.planType,
                    paymentPlanReference,
                    paymentPlan.scheduledPaymentAmount.get
                  )
                )
              )

            case _ =>
              logger.error(s"Unable to submit CancelPaymentPlanController missing PaymentPlanDetailsQuery or PaymentPlanReferenceQuery")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CancelPaymentPlanPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CancelPaymentPlanPage, NormalMode, updatedAnswers))
      )
  }
}
