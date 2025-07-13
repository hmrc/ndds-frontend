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
import forms.BankDetailsCheckYourAnswerFormProvider
import models.Mode
import navigation.Navigator
import pages.BankDetailsCheckYourAnswerPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.BankDetailsCheckYourAnswerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BankDetailsCheckYourAnswerController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: BankDetailsCheckYourAnswerFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: BankDetailsCheckYourAnswerView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(BankDetailsCheckYourAnswerPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      val list = SummaryListViewModel(
        rows = Seq(
          YourBankDetailsAccountHolderNameSummary.row(request.userAnswers),
          YourBankDetailsAccountNumberSummary.row(request.userAnswers),
          YourBankDetailsSortCodeSummary.row(request.userAnswers),
          YourBankDetailsAccountNumberSummary.row(request.userAnswers),
          YourBankDetailsNameSummary.row(request.userAnswers),
          YourBankDetailsAddressSummary.row(request.userAnswers),
        ).flatten
      )
      Ok(view(preparedForm, mode, list))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val list = SummaryListViewModel(
        rows = Seq(
          YourBankDetailsAccountHolderNameSummary.row(request.userAnswers),
          YourBankDetailsAccountNumberSummary.row(request.userAnswers),
          YourBankDetailsSortCodeSummary.row(request.userAnswers),
          YourBankDetailsAccountNumberSummary.row(request.userAnswers),
          YourBankDetailsNameSummary.row(request.userAnswers),
          YourBankDetailsAddressSummary.row(request.userAnswers),
        ).flatten
      )
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode,list))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BankDetailsCheckYourAnswerPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(BankDetailsCheckYourAnswerPage, mode, updatedAnswers))
      )
  }
}
