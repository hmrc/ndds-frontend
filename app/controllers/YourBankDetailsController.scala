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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{BarsService, LockService}
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
                                           barsService: BarsService,
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

      val credId = request.userId
      val personalOrBusinessOpt = request.userAnswers.get(PersonalOrBusinessAccountPage)

      personalOrBusinessOpt match {
        case None =>
          logger.warn(s"[YourBankDetailsController][onSubmit] Missing PersonalOrBusinessAccountPage for user $credId")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case Some(accountType) =>
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))
              ),
            bankDetails =>
              startVerification(accountType, bankDetails, request.userAnswers, credId, mode)
          )
      }
    }

  private def startVerification(
                                 accountType: PersonalOrBusinessAccount,
                                 bankDetails: YourBankDetails,
                                 userAnswers: UserAnswers,
                                 credId: String,
                                 mode: Mode
                               )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    barsService.barsVerification(accountType.toString, bankDetails).flatMap {
      case Right(response) =>
        // Step 1: BARS verification succeeded -> update answers with verified data
        onSuccessfulVerification(
          userAnswers,
          audisFlag   = false,
          bankDetails = bankDetails,
          bankName    = response.bank.map(_.bankName),
          bankAddress = response.bank.map(_.address)
        ).map { updatedAnswers =>
          Redirect(navigator.nextPage(YourBankDetailsPage, mode, updatedAnswers))
        }

      case Left(barsError) =>
        // Step 1: BARS verification failed -> handle error response
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
    // Step 1: Update UserAnswers with bank details + optional bank name/address
    val updatedAnswersTry = for {
      ua1 <- userAnswers.set(
        YourBankDetailsPage,
        YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(bankDetails, audisFlag, Some(false))
      )
      ua2 <- bankName.map(name => ua1.set(BankDetailsBankNamePage, name)).getOrElse(scala.util.Success(ua1))
      ua3 <- bankAddress.map(addr => ua2.set(BankDetailsAddressPage, addr)).getOrElse(scala.util.Success(ua2))
    } yield ua3

    // Step 2: Persist in session repo
    for {
      updatedAnswers <- Future.fromTry(updatedAnswersTry)
      _              <- sessionRepository.set(updatedAnswers)
    } yield {
      logger.info(s"[YourBankDetailsController][onSuccessfulVerification] Session repository updated successfully")
      updatedAnswers
    }
  }

  private def onFailedVerification(
                                    credId: String,
                                    bankDetails: YourBankDetails,
                                    mode: Mode,
                                    barsError: BarsErrors
                                  )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Result] = {

    // Step 1: Work out if this is an "unverified account" case
    val accountUnverifiedFlag: Boolean = barsError match {
      case BarsErrors.BankAccountUnverified => true
      case _                                => false
    }

    // Step 2: Invoke Lock Update Status (I4)
    for {
      lockResponse   <- lockService.updateLockForUser(credId)

      // Step 3a: Store accountUnverified in session
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AccountUnverifiedPage, accountUnverifiedFlag))
      _              <- sessionRepository.set(updatedAnswers)
    } yield {
      // Step 3b: Either show errors on the form or redirect to lock end-journey
      lockResponse.lockStatus match {
        case NotLocked =>
          val formWithErrors = BarsErrorMapper
            .toFormError(barsError)
            .foldLeft(form.fill(bankDetails))(_ withError _)

          BadRequest(view(formWithErrors, mode, routes.PersonalOrBusinessAccountController.onPageLoad(mode)))

        case LockedAndVerified | LockedAndUnverified =>
          Redirect("/todo-lock-end-journey") // TODO: replace with actual lock end-journey route
      }
    }
  }
}
