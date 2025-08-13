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
import forms.validation.BarsErrorMapper
import models.errors.BarsErrors.BankAccountUnverified
import models.requests.DataRequest
import models.{Mode, PersonalOrBusinessAccount, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.{PersonalOrBusinessAccountPage, YourBankDetailsPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{BARService, LockService}
import uk.gov.hmrc.http.HeaderCarrier
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
                                           barService: BARService,
                                           lockService: LockService,
                                           formProvider: YourBankDetailsFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: YourBankDetailsView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[YourBankDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(YourBankDetailsPage) match {
        case None => form
        case Some(value) => form.fill(YourBankDetailsWithAuddisStatus.toModelWithoutAuddisStatus(value))
      }

      Ok(view(preparedForm, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val personalOrBusinessOpt = request.userAnswers.get(PersonalOrBusinessAccountPage)
      val credId = request.userId // or however you get credId

      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))),

        bankDetails =>
          personalOrBusinessOpt match {
            case None =>
              Future.successful(Redirect(navigator.nextPage(YourBankDetailsPage, mode, request.userAnswers)))

            case Some(accountType) =>
              startVerification(accountType, bankDetails, request.userAnswers, credId, mode)
          }
      )
  }

  private def startVerification(accountType: PersonalOrBusinessAccount,
                                bankDetails: YourBankDetails,
                                userAnswers: UserAnswers,
                                credId: String,
                                mode: Mode
                               )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    barService.barsVerification(accountType.toString, bankDetails).flatMap {
      case Right(_) =>
        onSuccessfulVerification(userAnswers, false, bankDetails).map { updatedAnswers =>
          Redirect(navigator.nextPage(YourBankDetailsPage, mode, updatedAnswers))
        }

      case Left(_) =>
        onFailedVerification(credId, bankDetails, mode)
    }
  }

  private def onSuccessfulVerification(userAnswers: UserAnswers,
                                       audisFlag: Boolean,
                                       bankDetails: YourBankDetails): Future[UserAnswers] = {
    val updatedAnswers = userAnswers.set(
      YourBankDetailsPage,
      YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(bankDetails, audisFlag, Some(false))
    )

    Future.fromTry(updatedAnswers).flatMap { ua =>
      sessionRepository.set(ua).map(_ => ua)
    }
  }

  private def onFailedVerification(credId: String,
                                   bankDetails: YourBankDetails,
                                   mode: Mode
                                  )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    lockService.updateLockForUser(credId).map { _ =>
      println("********************lock called***************")
      val formWithErrors = BarsErrorMapper
        .toFormError(BankAccountUnverified)
        .foldLeft(form.fill(bankDetails))(_ withError _)

      BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode))) // check with shridhar 3rd parameter for view

    }
  }
}
