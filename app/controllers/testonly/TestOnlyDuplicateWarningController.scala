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

package controllers.testonly

import controllers.actions.*
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
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
import views.html.testonly.TestOnlyDuplicateWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyDuplicateWarningController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DuplicateWarningFormProvider,
  val controllerComponents: MessagesControllerComponents,
  nddsService: NationalDirectDebitService,
  view: TestOnlyDuplicateWarningView,
  chrisService: ChrisSubmissionForAmendService
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
          Redirect(controllers.routes.BackSubmissionController.onPageLoad())
        )
      } else {
        if (nddsService.amendPaymentPlanGuard(userAnswers)) {
          val maybeResult = for {
            updatedAnswers <- userAnswers.set(DuplicateWarningPage, true).toOption
          } yield {
            Ok(view(form, mode, testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()))
          }

          maybeResult match {
            case Some(result) =>
              sessionRepository
                .set(userAnswers.set(DuplicateWarningPage, true).get)
                .map(_ => result)

            case None =>
              logger.warn("Failed to set DuplicateWarningPage = true")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        } else {
          val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
          logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
          Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))
        }
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DuplicateWarningPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              result <-
                if (value) {
                  chrisService.submitToChris(
                    ua              = updatedAnswers,
                    successRedirect = Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanUpdateController.onPageLoad()),
                    errorRedirect   = Redirect(routes.JourneyRecoveryController.onPageLoad())
                  )
                } else {
                  Future.successful(
                    Redirect(testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad())
                  )
                }
            } yield result
        )
    }
}
