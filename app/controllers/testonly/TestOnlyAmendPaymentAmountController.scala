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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.routes
import controllers.testonly.routes as testOnlyRoutes
import forms.AmendPaymentAmountFormProvider
import models.Mode
import pages.{AmendPaymentAmountPage, ManagePaymentPlanTypePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.testonly.TestOnlyAmendPaymentAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyAmendPaymentAmountController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AmendPaymentAmountFormProvider,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyAmendPaymentAmountView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val answers = request.userAnswers

    if (nddsService.amendPaymentPlanGuard(answers)) {
      val preparedForm = answers.get(AmendPaymentAmountPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()))
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"[TestOnly] NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, testOnlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPaymentAmountPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(controllers.testonly.routes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad())
      )
  }
}
