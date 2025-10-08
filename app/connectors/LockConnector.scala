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

package connectors

import models.responses.LockResponse
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LockConnector @Inject() (config: ServicesConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  private val lockBaseUrl: String = config.baseUrl("lock") + "/locks/bars"

  def checkLock(credId: String)(implicit hc: HeaderCarrier): Future[LockResponse] =
    buildRequest(s"$lockBaseUrl/status", credId)

  def updateLock(credId: String)(implicit hc: HeaderCarrier): Future[LockResponse] =
    buildRequest(s"$lockBaseUrl/update", credId)

  def markUnverifiable(credId: String)(implicit hc: HeaderCarrier): Future[LockResponse] =
    buildRequest(s"$lockBaseUrl/markUnverifiable", credId)

  private def buildRequest[A](uri: String, credId: String)(implicit hc: HeaderCarrier): Future[LockResponse] =
    http
      .post(url"$uri")(hc)
      .withBody(Json.parse(s"""
           |{
           |  "identifier": "$credId"
           |}
           |""".stripMargin))
      .execute[Either[UpstreamErrorResponse, LockResponse]]
      .flatMap {
        case Right(resp) =>
          logger.info(s"""LockService Results:
               |{
               |  "identifier": "$credId"
               |}
               |""".stripMargin)
          logger.info(
            s"""LockService Results:
               |  • isLocked               = ${resp.isLocked}
               |  • unverifiable           = ${resp.unverifiable.getOrElse(false)}
               |  • verifyCalls            = ${resp.verifyCalls}
               |  . createdAt              = ${resp.createdAt}
               |  . lastUpdated            = ${resp.lastUpdated}
               |  • lockoutExpiryDateTime  = ${resp.lockoutExpiryDateTime.map(_.toString).getOrElse("n/a")}
               |""".stripMargin
          )
          logger.debug(s"Lock status: ${Json.stringify(Json.toJson(resp))}")
          Future.successful(resp)
        case Left(errorResponse) =>
          logger.warn(s"[LOCK] ${errorResponse.statusCode} ${errorResponse.message}")
          Future.failed(errorResponse)
      }
}
