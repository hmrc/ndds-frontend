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
import controllers.testonly.routes as testOnlyRoutes
import forms.DuplicateWarningFormProvider
import models.Mode
import pages.DuplicateWarningPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.testonly.TestOnlyDuplicateWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyDuplicateWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DuplicateWarningFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyDuplicateWarningView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val preparedForm = request.userAnswers.get(DuplicateWarningPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DuplicateWarningPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              if (value) {
                Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad())
              } else {
                Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad())
              }
            }
        )
    }
}
