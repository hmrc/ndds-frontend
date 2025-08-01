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
import forms.PersonalOrBusinessAccountFormProvider
import models.{Mode, PersonalOrBusinessAccount, UserAnswers}
import navigation.Navigator
import pages.PersonalOrBusinessAccountPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PersonalOrBusinessAccountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalOrBusinessAccountController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       formProvider: PersonalOrBusinessAccountFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PersonalOrBusinessAccountView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[PersonalOrBusinessAccount] = formProvider()
  val ddiCount: Int = 0
  
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) {
    implicit request =>
      val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val preparedForm = answers.get(PersonalOrBusinessAccountPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, routes.SetupDirectDebitPaymentController.onPageLoad(ddiCount)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, routes.SetupDirectDebitPaymentController.onPageLoad(ddiCount)))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(UserAnswers(request.userId)).set(PersonalOrBusinessAccountPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PersonalOrBusinessAccountPage, mode, updatedAnswers))
      )
  }
}
