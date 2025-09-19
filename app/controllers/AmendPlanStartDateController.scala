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

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.AmendPlanStartDatePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendPlanStartDateView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class AmendPlanStartDateController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 sessionRepository: SessionRepository,
                                                 navigator: Navigator,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 formProvider: AmendPlanStartDateFormProvider,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: AmendPlanStartDateView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => {
//        nddService.getEarliestPaymentDate(request.userAnswers) map { earliestPaymentDate =>
//          val isSinglePlan = nddService.isSinglePaymentPlan(request.userAnswers)

//          val form = formProvider(LocalDate.parse(earliestPaymentDate.date), isSinglePlan)
          val form = formProvider()
          val preparedForm = request.userAnswers.get(AmendPlanStartDatePage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
//          Ok(view(preparedForm, mode,
//            DateTimeFormats.formattedDateTimeShort(earliestPaymentDate.date),
//            DateTimeFormats.formattedDateTimeNumeric(earliestPaymentDate.date),
//            routes.AmendPaymentAmountController.onPageLoad(mode)
//          ))
//        } recover { case e =>
//          logger.warn(s"Unexpected error: $e")
//          Redirect(routes.JourneyRecoveryController.onPageLoad())
//        }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
//      nddService.getEarliestPaymentDate(request.userAnswers).flatMap { earliestPaymentDate =>
//        val isSinglePlan = nddService.isSinglePaymentPlan(request.userAnswers)
      val form = formProvider()
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPlanStartDatePage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(AmendPlanStartDatePage, mode, updatedAnswers))
      )
//        val form = formProvider(LocalDate.parse(earliestPaymentDate.date), isSinglePlan)

//        form.bindFromRequest().fold(
//          formWithErrors =>
//            nddService.getEarliestPaymentDate(request.userAnswers).map { earliestPaymentDate =>
//              BadRequest(view(formWithErrors, mode,
//                DateTimeFormats.formattedDateTimeShort(earliestPaymentDate.date),
//                DateTimeFormats.formattedDateTimeNumeric(earliestPaymentDate.date),
//                routes.AmendPaymentAmountController.onPageLoad(mode)
//              ))
//            },
//          value =>
//            for {
//              earliestPaymentDate <- nddService.getEarliestPaymentDate(request.userAnswers)
//              updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPlanStartDatePage, PaymentDateDetails(value, earliestPaymentDate.date)))
//              _ <- sessionRepository.set(updatedAnswers)
//            } yield Redirect(navigator.nextPage(AmendPlanStartDatePage, mode, updatedAnswers))
//        )
//      }.recoverWith {
//        case e =>
//          logger.warn(s"Unexpected error: $e")
//          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
//      }
  }
}
