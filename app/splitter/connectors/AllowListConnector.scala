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

package splitter.connectors

import com.google.inject.ImplementedBy
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.Logging
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpReadsInstances.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AllowListConnectorImpl])
trait AllowListConnector:
  def check(userId: String)(using HeaderCarrier): Future[Boolean]

class AllowListConnectorImpl @Inject() (config: ServicesConfig, httpClientV2: HttpClientV2)(using ExecutionContext)
    extends AllowListConnector,
      Logging:
  private val host = config.baseUrl("rate-limited-allow-list")
  private val url = url"$host/rate-limited-allow-list/services/ndds-frontend/features/private-beta-2026"

  override def check(userId: String)(using HeaderCarrier): Future[Boolean] =
    httpClientV2
      .post(url)
      .withBody(Json.obj("identifier" -> userId))
      .execute[HttpResponse]
      .map: response =>
        response.status match {
          case 200 =>
            response.json
              .validate[CheckResponse]
              .fold(
                errors => {
                  logger.error(s"Failed to parse response, errors $errors")
                  false
                },
                _.included
              )
          case x if x >= 400 && x < 500 =>
            logger.error(s"Error in request to downstream. Received response status code $x")
            false
          case x if x >= 500 =>
            logger.error(s"Error in downstream. Received response status code $x")
            false
          case x =>
            logger.error(s"Unexpected response status code from downstream. Received response status code $x")
            false
        }

case class CheckResponse(included: Boolean)

object CheckResponse:
  given Reads[CheckResponse] = Json.reads
