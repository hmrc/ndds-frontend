/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.PaymentDateFormProvider
import models.{Mode, PaymentDateDetails}
import navigation.Navigator
import pages.PaymentDatePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.PaymentDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PaymentDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentDateView,
  nddService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    {
      nddService.calculateFutureWorkingDays(request.userAnswers, request.userId) map { earliestPaymentDate =>
        val isSinglePlan = nddService.isSinglePaymentPlan(request.userAnswers) || nddService.isSinglePaymentPlanDirectDebitSource(request.userAnswers)
        val form = formProvider(LocalDate.parse(earliestPaymentDate.date), isSinglePlan)

        val preparedForm = request.userAnswers.get(PaymentDatePage) match {
          case None        => form
          case Some(value) => form.fill(value.enteredDate)
        }

        Ok(
          view(preparedForm,
               mode,
               DateTimeFormats.formattedDateTimeNumeric(earliestPaymentDate.date),
               routes.PaymentAmountController.onPageLoad(mode)
              )
        )
      } recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.SystemErrorController.onPageLoad())
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    nddService
      .calculateFutureWorkingDays(request.userAnswers, request.userId)
      .flatMap { earliestPaymentDate =>
        val isSinglePlan = nddService.isSinglePaymentPlan(request.userAnswers) || nddService.isSinglePaymentPlanDirectDebitSource(request.userAnswers)
        val form = formProvider(LocalDate.parse(earliestPaymentDate.date), isSinglePlan)

        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              nddService.calculateFutureWorkingDays(request.userAnswers, request.userId).map { earliestPaymentDate =>
                BadRequest(
                  view(formWithErrors,
                       mode,
                       DateTimeFormats.formattedDateTimeNumeric(earliestPaymentDate.date),
                       routes.PaymentAmountController.onPageLoad(mode)
                      )
                )
              },
            value =>
              for {
                earliestPaymentDate <- nddService.calculateFutureWorkingDays(request.userAnswers, request.userId)
                updatedAnswers      <- Future.fromTry(request.userAnswers.set(PaymentDatePage, PaymentDateDetails(value, earliestPaymentDate.date)))
                _                   <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(PaymentDatePage, mode, updatedAnswers))
          )
      }
      .recoverWith { case e =>
        logger.warn(s"Unexpected error: $e")
        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }
  }
}
