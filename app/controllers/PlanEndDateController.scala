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
import forms.PlanEndDateFormProvider
import models.{Mode, PlanStartDateDetails}
import navigation.Navigator
import pages.{PlanEndDatePage, PlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PlanEndDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlanEndDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PlanEndDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PlanEndDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(PlanStartDatePage) match {
      case Some(startDate) =>
        val earliestPlanStartDate = startDate.enteredDate
        val form = formProvider(startDate.enteredDate, earliestPlanStartDate)
        val preparedForm = request.userAnswers.get(PlanEndDatePage).map(Some(_)).fold(form)(form.fill)

        Ok(view(preparedForm, mode, routes.AddPaymentPlanEndDateController.onPageLoad(mode)))

      case None =>
        Redirect(routes.SystemErrorController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(PlanStartDatePage) match {
        case Some(startDate) =>
          val earliestPlanStartDate = startDate.enteredDate
          val form = formProvider(startDate.enteredDate, earliestPlanStartDate)

          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(view(formWithErrors, mode, routes.AddPaymentPlanEndDateController.onPageLoad(mode)))
                ),
              {
                case Some(endDate) =>
                  val updatedAnswers = request.userAnswers.set(PlanEndDatePage, endDate).get
                  sessionRepository.set(updatedAnswers).map { _ =>
                    Redirect(navigator.nextPage(PlanEndDatePage, mode, updatedAnswers))
                  }

                case None =>
                  val updatedAnswers = request.userAnswers.remove(PlanEndDatePage).get
                  sessionRepository.set(updatedAnswers).map { _ =>
                    Redirect(navigator.nextPage(PlanEndDatePage, mode, updatedAnswers))
                  }
              }
            )

        case _ =>
          logger.warn(s"Missing start date from session")
          Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }
    }

}
