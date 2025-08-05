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
import forms.PlanStartDateFormProvider
import models.{Mode, PlanStartDateDetails}
import navigation.Navigator
import pages.PlanStartDatePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import viewmodels.{PlanStartDateHelper, PlanStartDateViewModel}
import views.html.PlanStartDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlanStartDateController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: PlanStartDateFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: PlanStartDateView,
                                         planStartDateHelper: PlanStartDateHelper
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      val form = formProvider()
      val answers = request.userAnswers
      val preparedForm = answers.get(PlanStartDatePage) match {
        case None => form
        case Some(value) => form.fill(PlanStartDateDetails.toLocalDate(value))
      }
      for {
        earliestPlanStartDate <- planStartDateHelper.getEarliestPlanStartDate(request.userAnswers)
      } yield Ok(view(preparedForm, PlanStartDateViewModel(
        mode,
        DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
        DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date)
      )))
    } recover { case e =>
      logger.warn(s"Unexpected error: $e")
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            earliestPlanStartDate <- planStartDateHelper.getEarliestPlanStartDate(request.userAnswers)
          } yield BadRequest(view(formWithErrors, PlanStartDateViewModel(
            mode,
            DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
            DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date)
          ))),
        value =>
          for {
            earliestPlanStartDate <- planStartDateHelper.getEarliestPlanStartDate(request.userAnswers)
            updatedAnswers <- Future.fromTry(request.userAnswers
              .set(PlanStartDatePage, PlanStartDateDetails.toPaymentDatePageData(value, earliestPlanStartDate.date)))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PlanStartDatePage, mode, updatedAnswers))
      )
  }
}
