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

import models.Mode

import controllers.actions.*
import javax.inject.Inject
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.ManagePaymentPlanTypePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SuspendPaymentPlanView

import scala.concurrent.Future

class SuspendPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  view: SuspendPaymentPlanView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers

    if (nddsService.suspendPaymentPlanGuard(userAnswers)) {
      Future.successful(Ok(view(mode)))
    } else {
      logger.error(
        s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${userAnswers.get(ManagePaymentPlanTypePage)}"
      )
      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

}
