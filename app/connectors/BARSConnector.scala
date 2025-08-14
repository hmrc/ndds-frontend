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

import models.YourBankDetails
import models.requests.*
import models.responses.{Bank, BankAddress, BarsVerificationResponse, Country}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
case class BARSConnector @Inject()(
                                    config: ServicesConfig,
                                    http: HttpClientV2
                                  )(implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  private val barsBaseUrl: String = config.baseUrl("bars")
  private val personalUrl = "personal"
  private val businessUrl = "business"

  private val requestBodyPersonal = (bankDetails: YourBankDetails) =>
    BarsPersonalRequest(
      BarsAccount(bankDetails.sortCode, bankDetails.accountNumber),
      BarsSubject(bankDetails.accountHolderName)
    )

  private val requestBodyBusiness = (bankDetails: YourBankDetails) =>
    BarsBusinessRequest(
      BarsAccount(bankDetails.sortCode, bankDetails.accountNumber),
      BarsBusiness(bankDetails.accountHolderName)
    )

  /** Fetch bank metadata for given sort code */
  private def getMetadata(sortCode: String)(implicit hc: HeaderCarrier): Future[Option[Bank]] = {
    http
      .get(url"$barsBaseUrl/metadata/$sortCode")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            val json = response.json
            val addressJson = json \ "address"

            val linesSeq = (addressJson \ "lines").as[Seq[String]]
            val lines = linesSeq.mkString(", ")
            val town = (addressJson \ "town").as[String]
            val country = Country((addressJson \ "country" \ "name").as[String])
            val postCode = (addressJson \ "postCode").as[String]

            val address = BankAddress(lines, town, country, postCode)
            Some(Bank(
              name = (json \ "bankName").as[String],
              address = address
            ))

          case NOT_FOUND =>
            None // ✅ gracefully handle unknown sort codes
          case other =>
            throw new Exception(s"Unexpected status from metadata: $other")
        }
      }
  }

  def verify(isPersonal: Boolean, bankDetails: YourBankDetails)
            (implicit hc: HeaderCarrier): Future[BarsVerificationResponse] = {

    val verifyUrl = if (isPersonal) personalUrl else businessUrl

    val requestJson = if (isPersonal) {
      Json.toJson(requestBodyPersonal(bankDetails))
    } else {
      Json.toJson(requestBodyBusiness(bankDetails))
    }

    logger.info(s"Account validation called with $verifyUrl")

    http
      .post(url"$barsBaseUrl/verify/$verifyUrl")
      .withBody(requestJson)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) if response.status == OK =>
          Try(response.json.as[BarsVerificationResponse]) match {
            case Success(verificationData) =>
              getMetadata(bankDetails.sortCode).map { bank =>
                verificationData.copy(bank = bank)
              }

            case Failure(exception) =>
              logger.warn("Invalid JSON format in BARS response", exception)
              Future.failed(new Exception(s"Invalid JSON format: $exception"))
          }

        case Left(errorResponse) =>
          Future.failed(new Exception(
            s"Unexpected response: ${errorResponse.message}, status code: ${errorResponse.statusCode}"
          ))

        case Right(response) =>
          logger.warn(s"Unexpected status code from BARS: ${response.status}")
          Future.failed(new Exception(s"Unexpected status code: ${response.status}"))
      }
  }
}
