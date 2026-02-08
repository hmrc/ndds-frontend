/*
 * Copyright 2026 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.actions.*
import forms.DuplicateWarningForAddOrCreatePPFormProvider
import models.requests.ChrisSubmissionRequest
import models.responses.GenerateDdiRefResponse
import models.{Mode, UserAnswers}
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DuplicateWarningForAddOrCreatePPView
import queries.ExistingDirectDebitIdentifierQuery
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DuplicateWarningForAddOrCreatePPController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  nddService: NationalDirectDebitService,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DuplicateWarningForAddOrCreatePPFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DuplicateWarningForAddOrCreatePPView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(DuplicateWarningForAddOrCreatePPPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      implicit val ua: UserAnswers = request.userAnswers

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            if (value) {

              val ddiRefF: Future[String] =
                ua.get(ExistingDirectDebitIdentifierQuery)
                  .map(existing => Future.successful(existing.ddiRefNumber))
                  .getOrElse(
                    nddService
                      .generateNewDdiReference(required(PaymentReferencePage))
                      .map(_.ddiRefNumber)
                  )

              for {
                ddiRefNumber <- ddiRefF

                chrisRequest =
                  ChrisSubmissionRequest.buildChrisSubmissionRequest(
                    ua,
                    ddiRefNumber,
                    request.userId,
                    appConfig
                  )

                chrisSuccess <- nddService.submitChrisData(chrisRequest)

                result <-
                  if (chrisSuccess) {
                    for {
                      updated1 <- Future.fromTry(
                                    ua.set(CheckYourAnswerPage, GenerateDdiRefResponse(ddiRefNumber))
                                  )
                      updated2 <- Future.fromTry(
                                    updated1.set(CreateConfirmationPage, value)
                                  )
                      _ <- sessionRepository.set(updated2)
                    } yield Redirect(routes.DirectDebitConfirmationController.onPageLoad())
                  } else {
                    Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
                  }

              } yield result

            } else {
              for {
                updatedAnswers <- Future.fromTry(ua.set(CreateConfirmationPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(routes.CheckYourAnswersController.onPageLoad())
            }
        )
    }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))
}
