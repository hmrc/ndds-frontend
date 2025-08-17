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
import models.errors.BarsErrors
import models.errors.BarsErrors.BankAccountUnverified
import models.requests.DataRequest
import models.responses.BankAddress
import models.{Mode, PersonalOrBusinessAccount, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.{BankDetailsAddressPage, BankDetailsBankNamePage, PersonalOrBusinessAccountPage, YourBankDetailsPage}
import play.api.Logging
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
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[YourBankDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(YourBankDetailsPage) match {
        case None        => form
        case Some(value) => form.fill(YourBankDetailsWithAuddisStatus.toModelWithoutAuddisStatus(value))
      }
      Ok(view(preparedForm, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val personalOrBusinessOpt = request.userAnswers.get(PersonalOrBusinessAccountPage)
      val credId                = request.userId

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
          ),
        bankDetails =>
          personalOrBusinessOpt match {
            case None =>
              Future.successful(Redirect(navigator.nextPage(YourBankDetailsPage, mode, request.userAnswers)))

            case Some(accountType) =>
              startVerification(accountType, bankDetails, request.userAnswers, credId, mode)
          }
      )
    }

  private def startVerification(
                                 accountType: PersonalOrBusinessAccount,
                                 bankDetails: YourBankDetails,
                                 userAnswers: UserAnswers,
                                 credId: String,
                                 mode: Mode
                               )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    barService.barsVerification(accountType.toString, bankDetails).flatMap {
      case Right(response) =>
        onSuccessfulVerification(
          userAnswers,
          audisFlag = false,
          bankDetails,
          bankName = response.bank.map(_.name),
          bankAddress = response.bank.map(_.address)
        ).map { updatedAnswers =>
          Redirect(navigator.nextPage(YourBankDetailsPage, mode, updatedAnswers))
        }

      case Left(barsError) =>
        logger.warn(
          s"[YourBankDetailsController][startVerification] " +
            s"BARS verification failed for userId=$credId, accountType=$accountType. " +
            s"Reason: ${barsError.toString}"
        )
        onFailedVerification(credId, bankDetails, mode, barsError)
    }
  }


  private def onSuccessfulVerification(
                                        userAnswers: UserAnswers,
                                        audisFlag: Boolean,
                                        bankDetails: YourBankDetails,
                                        bankName: Option[String],
                                        bankAddress: Option[BankAddress]
                                      ): Future[UserAnswers] = {

    val updatedAnswersTry = for {
      ua1 <- userAnswers.set(
        YourBankDetailsPage,
        YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(bankDetails, audisFlag, Some(false))
      )
      ua2 <- bankName.map(name => ua1.set(BankDetailsBankNamePage, name)).getOrElse(scala.util.Success(ua1))
      ua3 <- bankAddress.map(addr => ua2.set(BankDetailsAddressPage, addr)).getOrElse(scala.util.Success(ua2))
    } yield ua3

    Future.fromTry(updatedAnswersTry).flatMap { ua =>
      sessionRepository.set(ua).map { _ =>
        logger.info(s"[YourBankDetailsController][onSuccessfulVerification] Session repository updated successfully")
        ua
      }
    }
  }

  private def onFailedVerification(
                                    credId: String,
                                    bankDetails: YourBankDetails,
                                    mode: Mode,
                                    barsError: BarsErrors
                                  )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    lockService.updateLockForUser(credId).map { _ =>
      val formWithErrors = BarsErrorMapper
        .toFormError(barsError)      // <- use the actual error now
        .foldLeft(form.fill(bankDetails))(_ withError _)
      BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
    }
  }

}
