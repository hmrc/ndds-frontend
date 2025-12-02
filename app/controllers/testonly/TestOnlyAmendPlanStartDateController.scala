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
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import forms.AmendPlanStartDateFormProvider
import models.{Mode, UserAnswers}
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.testonly.TestOnlyAmendPlanStartDateView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPlanStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: AmendPlanStartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPlanStartDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val answers = request.userAnswers

    if (nddsService.amendPaymentPlanGuard(answers)) {
      nddsService.calculateFutureWorkingDays(request.userAnswers, request.userId) map { earliestPlanStartDate =>
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
            testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )
        )
      } recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
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
                  testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
                )
              )
            ),
          value =>
            if (nddsService.amendPaymentPlanGuard(userAnswers))
              checkForDuplicate(mode, userAnswers, value)
            else {
              val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
              throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
              Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
            }
        )
    }
  }

  private def checkForDuplicate(mode: Mode, userAnswers: UserAnswers, value: LocalDate)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: Request[?]
  ): Future[Result] = {
    for {
      updatedAnswers         <- Future.fromTry(userAnswers.set(AmendPlanStartDatePage, value))
      _                      <- sessionRepository.set(updatedAnswers)
      duplicateCheckResponse <- nddsService.isDuplicatePaymentPlan(updatedAnswers)
    } yield {
      logger.warn(s"Duplicate check response is ${duplicateCheckResponse.isDuplicate}")
      if (duplicateCheckResponse.isDuplicate) {
        Redirect(testOnlyRoutes.TestOnlyDuplicateWarningController.onPageLoad(mode).url)
      } else {
        Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad().url)
      }
    }
  }
}
