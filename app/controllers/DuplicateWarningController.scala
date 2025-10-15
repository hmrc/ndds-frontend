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
import forms.DuplicateWarningFormProvider

import javax.inject.Inject
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.DuplicateWarningPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DuplicateWarningView

import scala.concurrent.{ExecutionContext, Future}

class DuplicateWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DuplicateWarningFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DuplicateWarningView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  private def backLink(from: Option[String], mode: Mode): Call = {
    from match {
      case Some("amendStartDate") => routes.AmendPlanStartDateController.onPageLoad(mode)
      case Some("amendEndDate")   => routes.AmendPlanEndDateController.onPageLoad(mode)
      case _                      => routes.PaymentPlanDetailsController.onPageLoad() // default fallback
    }
  }

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val from: Option[String] = request.getQueryString("from")

      val preparedForm = request.userAnswers.get(DuplicateWarningPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, backLink(from, mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val from: Option[String] = request.getQueryString("from")

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, backLink(from, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DuplicateWarningPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              if (value) {
                Redirect(routes.AmendPaymentPlanConfirmationController.onPageLoad(mode))
              } else {
                Redirect(routes.PaymentPlanDetailsController.onPageLoad())
              }
            }
        )
    }
}
