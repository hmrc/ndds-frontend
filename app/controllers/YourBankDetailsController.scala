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
import forms.YourBankDetailsFormProvider
import models.{Mode, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.YourBankDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YourBankDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourBankDetailsController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           sessionRepository: SessionRepository,
                                           navigator: Navigator,
                                           identify: IdentifierAction,
                                           requireData: DataRequiredAction,
                                           getData: DataRetrievalAction,
                                           formProvider: YourBankDetailsFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: YourBankDetailsView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[YourBankDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val answers = request.userAnswers
      val preparedForm = answers.get(YourBankDetailsPage) match {
        case None => form
        case Some(value) => form.fill(YourBankDetailsWithAuddisStatus.toModelWithoutAuddisStatus(value))
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            // TODO: Replace line below with response from real BARS service call
            // TODO: Check if this value actually comes from BARS
            barsServiceResponse <- Future.successful(false)
            updatedAnswers <- Future.fromTry(request.userAnswers
              .set(YourBankDetailsPage, YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(value, barsServiceResponse))
            )
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(YourBankDetailsPage, mode, updatedAnswers))
      )
  }
}
