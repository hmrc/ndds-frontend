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

import controllers.actions._
import forms.PaymentDateFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.PaymentDatePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentDateView

import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, Clock}
import java.time.format.DateTimeFormatter

class PaymentDateController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: PaymentDateFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       clock:Clock,
                                       view: PaymentDateView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val now: LocalDateTime = LocalDateTime.now(clock)
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  private val formattedDate = now.format(formatter)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val form = formProvider()

      val preparedForm = request.userAnswers.get(PaymentDatePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, formattedDate))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, formattedDate))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentDatePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PaymentDatePage, mode, updatedAnswers))
      )
  }
}
