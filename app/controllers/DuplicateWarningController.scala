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
import forms.DuplicateWarningFormProvider
import models.Mode
import pages.{DuplicateWarningPage, ManagePaymentPlanTypePage}
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{ChrisSubmissionForAmendService, NationalDirectDebitService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DuplicateWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DuplicateWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DuplicateWarningFormProvider,
  val controllerComponents: MessagesControllerComponents,
  nddsService: NationalDirectDebitService,
  chrisService: ChrisSubmissionForAmendService,
  view: DuplicateWarningView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val userAnswers = request.userAnswers

      val alreadyConfirmed =
        userAnswers.get(DuplicateWarningPage).contains(true)

      if (alreadyConfirmed) {
        logger.warn("Attempt to load this payment plan confirmation; redirecting to Page Not Found.")
        Future.successful(
          Redirect(routes.BackSubmissionController.onPageLoad())
        )
      } else {
        if (nddsService.amendPaymentPlanGuard(userAnswers)) {
          val maybeResult = for {
            updatedAnswers <- userAnswers.set(DuplicateWarningPage, true).toOption
          } yield {
            Ok(view(form, mode, routes.AmendPaymentPlanConfirmationController.onPageLoad()))
          }

          maybeResult match {
            case Some(result) =>
              sessionRepository
                .set(userAnswers.set(DuplicateWarningPage, true).get)
                .map(_ => result)

            case None =>
              logger.warn("Failed to set DuplicateWarningPage = true")
              Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
          }
        } else {
          val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
          logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
          Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
        }
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, routes.AmendPaymentPlanConfirmationController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DuplicateWarningPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              result <-
                if (value) {
                  chrisService.submitToChris(
                    ua              = updatedAnswers,
                    successRedirect = Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()),
                    errorRedirect   = Redirect(routes.SystemErrorController.onPageLoad())
                  )
                } else {
                  Future.successful(
                    Redirect(routes.AmendPaymentPlanConfirmationController.onPageLoad())
                  )
                }
            } yield result
        )
    }
}
