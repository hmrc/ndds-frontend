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
import models.requests.DataRequest
import models.responses.{BankAddress, LockedAndUnverified, LockedAndVerified, NotLocked}
import models.{Mode, PersonalOrBusinessAccount, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.{BarsService, LockService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Utils.LockExpirySessionKey
import views.html.YourBankDetailsView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourBankDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  requireData: DataRequiredAction,
  getData: DataRetrievalAction,
  barsService: BarsService,
  lockService: LockService,
  formProvider: YourBankDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YourBankDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

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

      val credId = request.userId
      val personalOrBusinessOpt = request.userAnswers.get(PersonalOrBusinessAccountPage)

      personalOrBusinessOpt match {
        case None =>
          logger.warn(s"Missing PersonalOrBusinessAccountPage for user: $credId")
          Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))

        case Some(accountType) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
                ),
              bankDetails => startVerification(accountType, bankDetails, request.userAnswers, credId, mode)
            )
      }
    }

  private def startVerification(
    accountType: PersonalOrBusinessAccount,
    bankDetails: YourBankDetails,
    userAnswers: UserAnswers,
    credId: String,
    mode: Mode
  )(implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Result] = {

    barsService.barsVerification(accountType.toString, bankDetails).flatMap {
      case Right((verificationResponse, bank)) =>
        onSuccessfulVerification(
          userAnswers,
          auddisFlag = bank.ddiVoucherFlag match {
            case Some("N") => true
            case _         => false
          },
          bankDetails = bankDetails,
          bankName    = bank.bankName,
          bankAddress = bank.address
        ).map { updatedAnswers =>
          Redirect(navigator.nextPage(YourBankDetailsPage, mode, updatedAnswers))
        }

      case Left(barsError) =>
        logger.warn(
          s"BARS verification failed for userId=$credId, accountType=$accountType. " +
            s"Reason: ${barsError.toString}"
        )
        onFailedVerification(credId, bankDetails, mode, barsError)
    }
  }

  private def onSuccessfulVerification(
    userAnswers: UserAnswers,
    auddisFlag: Boolean,
    bankDetails: YourBankDetails,
    bankName: String,
    bankAddress: BankAddress
  ): Future[UserAnswers] = {
    val updatedAnswersTry = for {
      ua1 <- userAnswers.set(
               YourBankDetailsPage,
               YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(bankDetails, auddisFlag, true)
             )
      ua2 <- ua1.set(BankDetailsBankNamePage, bankName)
      ua3 <- ua2.set(BankDetailsAddressPage, bankAddress)
    } yield ua3

    for {
      updatedAnswers <- Future.fromTry(updatedAnswersTry)
      _              <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
  }

  private def onFailedVerification(
    credId: String,
    bankDetails: YourBankDetails,
    mode: Mode,
    barsError: BarsErrors
  )(implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Result] = {

    val accountUnverifiedFlag: Boolean = barsError match {
      case BarsErrors.BankAccountUnverified => true
      case _                                => false
    }

    barsError match {
      case BarsErrors.SortCodeOnDenyList =>
        handleDenyListError(bankDetails, mode)
      case _ =>
        for {
          lockResponse   <- lockService.updateLockForUser(credId)
          updatedAnswers <- Future.fromTry(request.userAnswers.set(AccountUnverifiedPage, accountUnverifiedFlag))
          _              <- sessionRepository.set(updatedAnswers)
          result <- lockResponse.lockStatus match {
                      case NotLocked           => handleNotLocked(bankDetails, mode, barsError)
                      case LockedAndVerified   => handleLockedAndVerified(credId, accountUnverifiedFlag)
                      case LockedAndUnverified => handleLockedAndUnverified(credId)
                    }
        } yield result
    }
  }

  private def handleDenyListError(
    bankDetails: YourBankDetails,
    mode: Mode
  )(implicit request: DataRequest[?]): Future[Result] = {

    val formWithError = form
      .fill(bankDetails)
      .withError("sortCode", "yourBankDetails.error.sortCodeOnDenyList")

    Future.successful(
      BadRequest(view(formWithError, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
    )
  }

  private def handleNotLocked(bankDetails: YourBankDetails, mode: Mode, barsErrors: BarsErrors)(implicit request: DataRequest[?]): Future[Result] = {
    val formWithErrors = BarsErrorMapper
      .toFormError(barsErrors)
      .foldLeft(form.fill(bankDetails))(_ withError _)

    Future.successful(
      BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
    )
  }

  private def handleLockedAndVerified(credId: String, accountUnverifiedFlag: Boolean)(implicit
    hc: HeaderCarrier,
    request: DataRequest[?]
  ): Future[Result] = {

    if (accountUnverifiedFlag) {
      lockService
        .markUserAsUnverifiable(credId)
        .recoverWith { case e: UpstreamErrorResponse if e.statusCode == 409 => lockService.isUserLocked(credId) }
        .map(lockResp => withExpiry(Redirect(routes.AccountDetailsNotVerifiedController.onPageLoad()), lockResp.lockoutExpiryDateTime))
    } else {
      lockService
        .isUserLocked(credId)
        .map(lockResp => withExpiry(Redirect(routes.ReachedLimitController.onPageLoad()), lockResp.lockoutExpiryDateTime))
    }
  }

  private def handleLockedAndUnverified(credId: String)(implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Result] = {
    for {
      status <- lockService.isUserLocked(credId)
    } yield withExpiry(
      Redirect(routes.AccountDetailsNotVerifiedController.onPageLoad()),
      status.lockoutExpiryDateTime
    )
  }

  private def withExpiry(result: Result, expiry: Option[Instant])(implicit request: RequestHeader): Result =
    expiry match {
      case Some(dateTime) => result.addingToSession(LockExpirySessionKey -> dateTime.toString)(request)
      case None           => result
    }
}
