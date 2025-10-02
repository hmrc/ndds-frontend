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
import forms.AmendPlanEndDateFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, UpdatedAmendPlanEndDatePage, UpdatedAmendPaymentAmountPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendPlanEndDateView

import scala.concurrent.{ExecutionContext, Future}

class AmendPlanEndDateController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: AmendPlanEndDateFormProvider,
                                        nddService: NationalDirectDebitService,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: AmendPlanEndDateView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val form = formProvider()
      val preparedForm =
        request.userAnswers.get(UpdatedAmendPlanEndDatePage)
          .orElse(request.userAnswers.get(AmendPlanEndDatePage))
          .fold(form)(form.fill)

      Ok(view(preparedForm, mode,routes.AmendPaymentAmountController.onPageLoad(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode,routes.AmendPaymentAmountController.onPageLoad(mode)))),

        value =>
          for {
//            updatedAnswers  <- Future.fromTry(request.userAnswers.set(UpdatedAmendPlanEndDatePage, value))
            updatedAnswers    <- request.userAnswers
                                .set(UpdatedAmendPlanEndDatePage, value)
                                .map(nddService.isAmendmentMade)
                                .getOrElse(Future.successful(request.userAnswers))
            _               <- sessionRepository.set(updatedAnswers)
          } yield {
            println(s"*********** updatedAnswers: ${updatedAnswers}")
            if (updatedAnswers.get(AmendPaymentAmountPage) != updatedAnswers.get(UpdatedAmendPaymentAmountPage) ||
              updatedAnswers.get(AmendPlanEndDatePage) != updatedAnswers.get(UpdatedAmendPlanEndDatePage)) {
              println("Amount Amended")
              Redirect(navigator.nextPage(UpdatedAmendPlanEndDatePage, mode, updatedAnswers))
            } else {
              println("Amount Not Amended")
              val key = "amendment.noChange"
              val errorForm = form.fill(value).withError("value", key)
              BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
            }
          }
      )
  }
}
