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

import models.responses.{Bank, BarsVerificationResponse}
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class BarsConnector @Inject()(
                                    config: ServicesConfig,
                                    http: HttpClientV2
                                  )(implicit ec: ExecutionContext)
  extends HttpReadsInstances with Logging {
  private val barsBaseUrl: String = config.baseUrl("bars")

  private def getMetadata(sortCode: String)(implicit hc: HeaderCarrier): Future[Option[Bank]] = {
    http
      .get(url"$barsBaseUrl/metadata/$sortCode")
      .execute[Either[UpstreamErrorResponse, Bank]]
      .flatMap {
        case Right(bank) => Future.successful(Some(bank))
        case Left(err) if err.statusCode == NOT_FOUND =>
          Future.successful(None) // gracefully handle unknown sort codes
        case Left(err) =>
          Future.failed(new Exception(s"Unexpected error from metadata: ${err.statusCode} - ${err.message}"))
      }
  }


  def verify(verifyUrl: String, requestJson: JsValue)
            (implicit hc: HeaderCarrier): Future[BarsVerificationResponse] = {
    logger.info(s"Account validation called with $verifyUrl")
    http
      .post(url"$barsBaseUrl/verify/$verifyUrl")
      .withBody(requestJson)
      .execute[Either[UpstreamErrorResponse, BarsVerificationResponse]]
      .flatMap {
        case Right(verificationData) =>
          getMetadata((requestJson \ "account" \ "sortCode").as[String]).map { bank =>
            verificationData.copy(bank = bank)
          }

        case Left(errorResponse) =>
          Future.failed(new Exception(
            s"Unexpected response: ${errorResponse.message}, status code: ${errorResponse.statusCode}"
          ))
      }
  }

}
