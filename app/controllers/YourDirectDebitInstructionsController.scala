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

import config.FrontendAppConfig
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DirectDebitDetailsData
import views.html.YourDirectDebitInstructionsView

import javax.inject.Inject

class YourDirectDebitInstructionsController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: YourDirectDebitInstructionsView,
                                                       appConfig: FrontendAppConfig
                                                     )
  extends FrontendBaseController with I18nSupport with DirectDebitDetailsData {

  def onPageLoad: Action[AnyContent] = (identify andThen getData) { implicit request =>
    val maxLimitReached = directDebitDetailsData.length > appConfig.maxNumberDDIsAllowed
    Ok(view(directDebitDetailsData, maxLimitReached))
  }
}
