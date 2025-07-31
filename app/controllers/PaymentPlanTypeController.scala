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
import forms.PaymentPlanTypeFormProvider
import models.{DirectDebitSource, Mode, PaymentPlanType}
import navigation.Navigator
import pages.{DirectDebitSourcePage, PaymentPlanTypePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentPlanTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanTypeController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: PaymentPlanTypeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PaymentPlanTypeView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[PaymentPlanType] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val answers = request.userAnswers
      val selectedAnswers = answers.get(DirectDebitSourcePage)

      val preparedForm = answers.get(PaymentPlanTypePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode, selectedAnswers))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val answers = request.userAnswers
      val selectedAnswers = answers.get(DirectDebitSourcePage)

      form.bindFromRequest().fold(
        formWithErrors =>
          logger.warn(s"Payment plan validation error: ${formWithErrors}")
          Future.successful(BadRequest(view(formWithErrors, mode, selectedAnswers))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentPlanTypePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PaymentPlanTypePage, mode, updatedAnswers))
      )
  }
}
