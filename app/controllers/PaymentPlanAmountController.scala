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
import forms.amend.PaymentPlanAmountFormProvider
import models.Mode
import navigation.Navigator
import pages.amend.PaymentPlanAmountPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.PaymentPlanAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanAmountController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: PaymentPlanAmountFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: PaymentPlanAmountView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val answers = request.userAnswers
      val preparedForm = answers.get(PaymentPlanAmountPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      //TODO: Change the route to PP1 screen once built
      Ok(view(preparedForm, mode, routes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          //TODO: Change the route to PP1 screen once built
          Future.successful(BadRequest(view(formWithErrors, mode, routes.JourneyRecoveryController.onPageLoad()))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanAmountPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PaymentPlanAmountPage, mode, updatedAnswers))
      )
  }

}
