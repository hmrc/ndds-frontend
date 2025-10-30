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
import config.FrontendAppConfig
import pages.ManagePaymentPlanTypePage
import models.PaymentPlanType
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendingPaymentPlanView

class AmendingPaymentPlanController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AmendingPaymentPlanView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
    val p1Key = planType match {
      case s if s == PaymentPlanType.SinglePaymentPlan.toString => "amendingPaymentPlan.p1.single"
      case s if s == PaymentPlanType.BudgetPaymentPlan.toString => "amendingPaymentPlan.p1.budget"
      case _                                                    => "amendingPaymentPlan.p1.single"
    }
    Ok(view(appConfig.hmrcHelplineUrl, p1Key))
  }
}
