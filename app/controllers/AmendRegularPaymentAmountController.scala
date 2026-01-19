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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.RegularPaymentAmountFormProvider
import models.Mode
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CurrentPageQuery, PaymentPlanDetailsQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendRegularPaymentAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendRegularPaymentAmountController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RegularPaymentAmountFormProvider,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: AmendRegularPaymentAmountView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val answers = request.userAnswers

    if (nddsService.amendPaymentPlanGuard(answers)) {
      val existingAmount = answers
        .get(AmendPaymentAmountPage)
        .orElse(answers.get(PaymentPlanDetailsQuery).flatMap(_.paymentPlanDetails.scheduledPaymentAmount))

      val preparedForm = existingAmount match {
        case Some(value) => form.fill(value)
        case None        => form
      }

      Ok(view(preparedForm, mode, routes.AmendingPaymentPlanController.onPageLoad()))
    } else {
      val planType = answers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Redirect(routes.SystemErrorController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, routes.AmendingPaymentPlanController.onPageLoad()))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPaymentAmountPage, value))
            updatedAnswers <- Future.fromTry(updatedAnswers.set(CurrentPageQuery, request.uri))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.AmendPaymentPlanConfirmationController.onPageLoad())
      )
  }
}
