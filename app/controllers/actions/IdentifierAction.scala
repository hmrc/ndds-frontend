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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {

  val acceptedEnrolments = Set(
    "IR-SA",
    "IR-SA-TRUST-ORG",
    "IR-SA-PART-ORG",
    "IR-CT",
    "IR-PAYE",
    "HMRC-CIS-ORG",
    "HMRC-MGD-ORG",
    "HMRC-ECL-ORG"
  )

  private def usingSupportedEnrolments(enrolments: Enrolments): Boolean = {
    enrolments.enrolments
      .exists {
        case e @ Enrolment("HMRC-PSA-ORG", _, _, _) if e.isActivated                        => true
        case e @ Enrolment(key, _, state, _) if e.isActivated || state == "NotYetActivated" => acceptedEnrolments(key)
        case _                                                                              => false
      }
  }

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.credentials and Retrievals.allEnrolments) {
      case Some(credentials) ~ userEnrolments =>
        if (usingSupportedEnrolments(userEnrolments)) {
          block(IdentifierRequest(request, credentials.providerId))
        } else {
          Future.successful(Redirect(routes.AccessDeniedController.onPageLoad()))
        }
      case _ => throw new UnauthorizedException("Unable to retrieve credential id")
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }

  }
}

class SessionIdentifierAction @Inject() (
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    hc.sessionId match {
      case Some(session) =>
        block(IdentifierRequest(request, session.value))
      case None =>
        Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
    }
  }
}
