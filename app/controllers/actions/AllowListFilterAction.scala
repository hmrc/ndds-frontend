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

import models.requests.IdentifierRequest
import play.api.mvc.{ActionFilter, Result}
import play.api.{Configuration, Logging}
import play.api.mvc.Results.SeeOther
import splitter.connectors.AllowListConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AllowListFilterAction @Inject() (connector: AllowListConnector, configuration: Configuration)(implicit val executionContext: ExecutionContext)
    extends ActionFilter[IdentifierRequest]
    with Logging {

  private val legacyStartUrl = configuration.get[String]("microservice.services.ndds-legacy.path")

  override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] =
    implicit val hc: HeaderCarrier = HeaderCarrier()
    connector
      .check(request.userId)
      .map:
        case true  => None
        case false => Some(SeeOther(legacyStartUrl))
      .recover:
        case NonFatal(e) =>
          logger.error("Error when checking for user id ", e)
          Some(SeeOther(legacyStartUrl))
}
