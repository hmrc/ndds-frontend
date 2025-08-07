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
import pages.{DirectDebitSourcePage, PlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RDSDatacacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.PlanStartDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                                         rdsDatacacheService: RDSDatacacheService
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      val answers = request.userAnswers

      rdsDatacacheService.getEarliestPlanStartDate(request.userAnswers) map { earliestPlanStartDate =>
        val earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val form = formProvider(answers, earliestDate)
        val preparedForm = answers.get(PlanStartDatePage) match {
          case None => form
          case Some(value) => form.fill(value.enteredDate)
        }

        Ok(view(
          preparedForm,
          mode,
          DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
          DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
          request.userAnswers.get(DirectDebitSourcePage).getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers"))
        ))
      } recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        earliestPlanStartDate <- rdsDatacacheService.getEarliestPlanStartDate(request.userAnswers)
        earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
        form = formProvider(request.userAnswers, earliestDate)
        result <- form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              formWithErrors,
              mode,
              DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
              DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
              request.userAnswers.get(DirectDebitSourcePage).getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers"))
            ))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers
                .set(PlanStartDatePage, PlanStartDateDetails(value, earliestPlanStartDate.date)))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(PlanStartDatePage, mode, updatedAnswers))
        )
      } yield result).recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }
}
