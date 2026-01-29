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

package splitter.controllers

import controllers.actions.IdentifierAction
import play.api.mvc.*
import play.api.Configuration
import splitter.connectors.AllowListConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SplitterController @Inject() (identify: IdentifierAction,
                                    connector: AllowListConnector,
                                    configuration: Configuration,
                                    val controllerComponents: MessagesControllerComponents
                                   )(using ExecutionContext)
    extends FrontendBaseController:

  private val legacyStartUrl = configuration.get[String]("microservice.services.ndds-legacy.path")
  private lazy val nddsFrontendStartUrl = controllers.routes.LandingController.onPageLoad()

  def redirect(path: String): Action[AnyContent] = identify.async:
    implicit req =>
      connector
        .check(req.userId)
        .map:
          case true  => SeeOther(nddsFrontendStartUrl.url)
          case false => SeeOther(legacyStartUrl)
