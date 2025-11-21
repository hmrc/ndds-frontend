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
import forms.ConfirmAuthorityFormProvider
import models.{ConfirmAuthority, Mode, UserAnswers}
import navigation.Navigator
import pages.ConfirmAuthorityPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmAuthorityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmAuthorityController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: ConfirmAuthorityFormProvider,
  requireData: DataRequiredAction,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  view: ConfirmAuthorityView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form: Form[ConfirmAuthority] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData) { implicit request =>
      val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val preparedForm = answers.get(ConfirmAuthorityPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode, routes.BankDetailsCheckYourAnswerController.onPageLoad(mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, routes.SetupDirectDebitPaymentController.onPageLoad()))),
        value => {
          val updatedTry = request.userAnswers.set(ConfirmAuthorityPage, value)
          for {
            updated <- Future.fromTry(updatedTry)
            _       <- sessionRepository.set(updated)
          } yield {
            Redirect(navigator.nextPage(ConfirmAuthorityPage, mode, updated))
          }
        }
      )
  }
}
