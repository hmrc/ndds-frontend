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

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Configuration
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IdentityIdentifierAction extends IdentifierAction, AuthorisedFunctions

class IdentityIdentifierActionImpl @Inject()(
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  configuration: Configuration,
  val parser: BodyParsers.Default
)(using val executionContext: ExecutionContext) extends IdentityIdentifierAction {

  private val allowListChecksEnabled: Boolean = configuration.get[Boolean]("features.allowListChecksEnabled")
  private val legacyStartUrl = configuration.get[String]("microservice.services.ndds-legacy.path")

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if allowListChecksEnabled then
      authorised().retrieve(Retrievals.credentials) {
        case Some(credentials)         => block(IdentifierRequest(request, credentials.providerId))
        case None                      => throw new UnauthorizedException("Unable to retrieve credential id")
      } recover {
        case _: NoActiveSession        => Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException => Redirect(routes.UnauthorisedController.onPageLoad())
      }
    else
      Future.successful(Redirect(legacyStartUrl))
}