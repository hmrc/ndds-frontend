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

package testOnly.controllers

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.mvc.AnyContent
import testOnly.views.html.{IAmBtaPage, IAmPtaPage}
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendController}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
abstract class DirectDebitConfirmationController @Inject() (
  iAmBtaPage: IAmBtaPage,
  iAmPtaPage: IAmPtaPage
)(using ExecutionContext, FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  val showBtaPage: Action[AnyContent] = Action { implicit request =>
    Ok(iAmBtaPage())
  }

  val showPtaPage: Action[AnyContent] = Action { implicit request =>
    Ok(iAmPtaPage())
  }

}
