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
import forms.AmendPlanStartDateFormProvider
import models.Mode
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.CurrentPageQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.AmendPlanStartDateView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendPlanStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanStartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AmendPlanStartDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val answers = request.userAnswers

    if (nddsService.isSinglePaymentPlan(answers)) {
      nddsService.getFutureWorkingDays(request.userAnswers, request.userId) map { earliestPlanStartDate =>
        val earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val form = formProvider(answers, earliestDate)
        val preparedForm = request.userAnswers
          .get(AmendPlanStartDatePage)
          .orElse(request.userAnswers.get(AmendPlanStartDatePage))
          .fold(form)(form.fill)

        Ok(
          view(
            preparedForm,
            mode,
            DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
            routes.AmendingPaymentPlanController.onPageLoad()
          )
        )
      } recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.SystemErrorController.onPageLoad())
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    nddsService.calculateFutureWorkingDays(request.userAnswers, request.userId) flatMap { earliestPlanStartDate =>
      val earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
      val userAnswers = request.userAnswers
      val form = formProvider(userAnswers, earliestDate)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  mode,
                  DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
                  routes.AmendingPaymentPlanController.onPageLoad()
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(userAnswers.set(AmendPlanStartDatePage, value))
              updatedAnswers <- Future.fromTry(updatedAnswers.set(CurrentPageQuery, request.uri))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(routes.AmendPaymentPlanConfirmationController.onPageLoad())
            }
        )
    }
  }
}
