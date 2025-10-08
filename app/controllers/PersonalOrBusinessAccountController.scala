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
import forms.PersonalOrBusinessAccountFormProvider
import models.{Mode, PersonalOrBusinessAccount, UserAnswers}
import navigation.Navigator
import pages.{BankDetailsAddressPage, BankDetailsBankNamePage, PersonalOrBusinessAccountPage, YourBankDetailsPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PersonalOrBusinessAccountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class PersonalOrBusinessAccountController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: PersonalOrBusinessAccountFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PersonalOrBusinessAccountView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form: Form[PersonalOrBusinessAccount] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val answers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val preparedForm = answers.get(PersonalOrBusinessAccountPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, routes.SetupDirectDebitPaymentController.onPageLoad()))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, mode, routes.SetupDirectDebitPaymentController.onPageLoad()))
          ),
        value => {
          val originalAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

          val updatedAnswersTry = updateAnswers(originalAnswers, value)

          for {
            updatedAnswers <- Future.fromTry(updatedAnswersTry)
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PersonalOrBusinessAccountPage, mode, updatedAnswers))
        }
      )
  }

  private def updateAnswers(
    userAnswers: UserAnswers,
    newValue: PersonalOrBusinessAccount
  ): Try[UserAnswers] = {
    val oldValue = userAnswers.get(PersonalOrBusinessAccountPage)

    if (oldValue.contains(newValue)) {
      Success(userAnswers)
    } else {
      userAnswers
        .remove(BankDetailsAddressPage)
        .flatMap(_.remove(BankDetailsBankNamePage))
        .flatMap(_.remove(YourBankDetailsPage))
        .flatMap(_.set(PersonalOrBusinessAccountPage, newValue))
    }
  }

}
