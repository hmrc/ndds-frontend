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

import base.SpecBase
import config.FrontendAppConfig
import models.requests.WorkingDaysOffsetRequest
import models.responses.EarliestPaymentDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future
import scala.language.postfixOps

class RdsDataCacheConnectorSpec extends SpecBase with MockitoSugar {

  val mockConfig = mock[FrontendAppConfig]
  val mockHttpClient = mock[HttpClientV2]
  val mockRequestBuilder = mock[RequestBuilder]
  val connector = new RdsDataCacheConnector(config = mockConfig, httpClientV2 = mockHttpClient)
  val expectedOffset = 5

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConfig)
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  val date = LocalDate.now()

  def setupMocks(testRequest: WorkingDaysOffsetRequest) = {
    when(mockConfig.earliestPaymentDateUrl).thenReturn("http://localhost:6990/rds-datacache-proxy/direct-debits/earliest-payment-date")
    when(mockConfig.paymentDelayFixed).thenReturn(2)
    when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
    when(mockConfig.earliestPaymentDateUrl).thenReturn("http://localhost:6990/rds-datacache-proxy/direct-debits/earliest-payment-date")
    when(connector.httpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(testRequest)))(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "getEarliestPaymentDate" - {
    "must successfully retrieve a date" in {
      val testRequest = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = "5")

      setupMocks(testRequest)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, Json.toJson(EarliestPaymentDate(date.plusDays(expectedOffset).toString)).toString))))

      val expectedResult = EarliestPaymentDate(date = date.plusDays(expectedOffset).toString)
      val result = connector.getEarliestPaymentDate(testRequest).futureValue

      result mustEqual expectedResult
    }

    "must fail when the result is parsed as a HttpResponse but is not a 200 (OK) response" in {
      val testRequest = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = "5")

      setupMocks(testRequest)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(CREATED, Json.toJson(EarliestPaymentDate(date.plusDays(expectedOffset).toString)).toString))))

      val result = intercept[Exception](connector.getEarliestPaymentDate(testRequest).futureValue)

      result.getMessage must include("Unexpected status code: 201")
    }

    "must fail when the result is parsed as an UpstreamErrorResponse" in {
      val testRequest = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = "5")

      setupMocks(testRequest)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse(statusCode = INTERNAL_SERVER_ERROR, message = "test error"))))

      val result = intercept[Exception](connector.getEarliestPaymentDate(testRequest).futureValue)

      result.getMessage must include("Unexpected response: test error, status code: 500")
    }
    "must fail when the result is a failed future" in {

      val testRequest = WorkingDaysOffsetRequest(baseDate = "2024-12-25", offsetWorkingDays = "5")

      setupMocks(testRequest)

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.failed(new Exception("bang")))

      val result = intercept[Exception](connector.getEarliestPaymentDate(testRequest).futureValue)

      result.getMessage must include("bang")
    }
  }

}
