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

import models.NddResponse
import models.requests.{ChrisSubmissionRequest, GenerateDdiRefRequest, WorkingDaysOffsetRequest}
import models.responses.{EarliestPaymentDate, GenerateDdiRefResponse, NddDDPaymentPlansResponse}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class NationalDirectDebitConnector @Inject()(config: ServicesConfig,
                                             http: HttpClientV2)
                                            (implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  private val nationalDirectDebitBaseUrl: String = config.baseUrl("national-direct-debit") + "/national-direct-debit"

  def retrieveDirectDebits()(implicit hc: HeaderCarrier): Future[NddResponse] = {
    http.get(url"$nationalDirectDebitBaseUrl/direct-debits")(hc)
      .execute[NddResponse]
  }

  def getEarliestPaymentDate(workingDaysOffsetRequest: WorkingDaysOffsetRequest)
                            (implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    http
      .post(url"$nationalDirectDebitBaseUrl/direct-debits/future-working-days")
      .withBody(Json.toJson(workingDaysOffsetRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) if response.status == OK =>
          Try(response.json.as[EarliestPaymentDate]) match {
            case Success(data) => Future.successful(data)
            case Failure(exception) => Future.failed(new Exception(s"Invalid JSON format $exception"))
          }
        case Left(errorResponse) =>
          Future.failed(new Exception(s"Unexpected response: ${errorResponse.message}, status code: ${errorResponse.statusCode}"))
        case Right(response) => Future.failed(new Exception(s"Unexpected status code: ${response.status}"))
      }
  }

  def submitChrisData(submission: ChrisSubmissionRequest)
                     (implicit hc: HeaderCarrier): Future[Boolean] = {
    http.post(url"$nationalDirectDebitBaseUrl/chris")
      .withBody(Json.toJson(submission))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) if response.status == OK =>
          Future.successful(true)
        case Left(errorResponse) =>
          logger.error(s"CHRIS submission failed: ${errorResponse.message}, status: ${errorResponse.statusCode}")
          Future.failed(new Exception(s"CHRIS submission failed: ${errorResponse.message}, status: ${errorResponse.statusCode}"))
        case Right(response) =>
          logger.error(s"Unexpected CHRIS response status: ${response.status}")
          Future.failed(new Exception(s"Unexpected status: ${response.status}"))
      }
  }


  def generateNewDdiReference(generateDdiRefRequest: GenerateDdiRefRequest)
                             (implicit hc: HeaderCarrier): Future[GenerateDdiRefResponse] = {
    http
      .post(url"$nationalDirectDebitBaseUrl/direct-debit-reference")
      .withBody(Json.toJson(generateDdiRefRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) if response.status == OK =>
          Try(response.json.as[GenerateDdiRefResponse]) match {
            case Success(data) => Future.successful(data)
            case Failure(exception) => Future.failed(new Exception(s"Invalid JSON format $exception"))
          }
        case Left(errorResponse) =>
          Future.failed(new Exception(s"Unexpected response: ${errorResponse.message}, status code: ${errorResponse.statusCode}"))
        case Right(response) => Future.failed(new Exception(s"Unexpected status code: ${response.status}"))
      }
  }

  def retrieveDirectDebitPaymentPlans(directDebitReference: String)(implicit hc: HeaderCarrier): Future[NddDDPaymentPlansResponse] = {
    http.get(url"$nationalDirectDebitBaseUrl/direct-debits/$directDebitReference/payment-plans")(hc)
      .execute[NddDDPaymentPlansResponse]
  }
}
