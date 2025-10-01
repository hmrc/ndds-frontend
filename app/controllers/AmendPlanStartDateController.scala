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
import pages.{AmendPlanStartDatePage, NewAmendPlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
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
                                                 nddService: NationalDirectDebitService,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: AmendPlanStartDateView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => {
      val form = formProvider()
      val preparedForm = request.userAnswers.get(NewAmendPlanStartDatePage)
        .orElse(request.userAnswers.get(AmendPlanStartDatePage))
        .fold(form)(form.fill)

      Ok(view(preparedForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

          val form = formProvider()
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(NewAmendPlanStartDatePage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield {
                if (nddService.amendmentMade(updatedAnswers)) {
                  //TODO: will be used to show a warning screen later for amending duplicate plan
                  val flag = nddService.isDuplicatePaymentPlan(updatedAnswers)
                  flag.map { value =>
                    println(s"Duplicate check response is $value")
                  }
                  Redirect(navigator.nextPage(AmendPlanStartDatePage, mode, updatedAnswers))
                } else {
                  val key = "amendment.noChange"
                  val errorForm = form.fill(value).withError("value", key)
                  BadRequest(view(errorForm, mode, routes.AmendPaymentAmountController.onPageLoad(mode)))
                }
              }
          )
      }
  }
