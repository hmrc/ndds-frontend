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

package controllers.start

import play.api.mvc.*
import play.api.{Configuration, Logging}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StartController @Inject() (
  configuration: Configuration,
  val controllerComponents: MessagesControllerComponents
)(using ExecutionContext)
    extends FrontendBaseController,
      Logging:

  private lazy val nddsFrontendStartUrl = controllers.routes.LandingController.onPageLoad()

  def redirect(path: String): Action[AnyContent] = Action:
    SeeOther(nddsFrontendStartUrl.url)
