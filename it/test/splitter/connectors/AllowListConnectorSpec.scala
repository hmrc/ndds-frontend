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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class AllowListConnectorSpec extends AnyFreeSpec, Matchers, WireMockSupport,  HttpClientV2Support, TryValues, OptionValues, ScalaFutures, IntegrationPatience, BeforeAndAfterEach:
  val path =  "/rate-limited-allow-list/services/ndds-frontend/features/private-beta-2026"
  val config = ServicesConfig(Configuration.from(Map(
    "microservice.services.rate-limited-allow-list.host" -> wireMockHost,
    "microservice.services.rate-limited-allow-list.port" -> wireMockPort.toString,
    "microservice.services.rate-limited-allow-list.path" -> path
  )))
  val connector = AllowListConnectorImpl(config, httpClientV2)

  given HeaderCarrier = HeaderCarrier()

  "return true, when the response is a 200 with a passing check" in {
    val identifierValue = "a user identifier"

    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.obj("identifier" -> identifierValue).toString))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.obj("included" -> true).toString)
        )
    )

    val result = connector.check(identifierValue).futureValue

    result mustEqual true
  }

  "return false, when the response is a 200 with a failing check" in {
    val identifierValue = "a user identifier"

    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.obj("identifier" -> identifierValue).toString))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.obj("included" -> false).toString)
        )
    )

    val result = connector.check(identifierValue).futureValue

    result mustEqual false
  }

  "return false, when the response is a 400" in {
    val identifierValue = "a user identifier"

    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.obj("identifier" -> identifierValue).toString))
        .willReturn(
          aResponse()
            .withStatus(400)
        )
    )

    val result = connector.check(identifierValue).futureValue

    result mustEqual false
  }

  "return false, when the response is a 5xx" in {
    val identifierValue = "a user identifier"

    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.obj("identifier" -> identifierValue).toString))
        .willReturn(
          aResponse()
            .withStatus(500)
        )
    )

    val result = connector.check(identifierValue).futureValue

    result mustEqual false
  }

  "return false, for an unexpected response status" in {
    val identifierValue = "a user identifier"

    stubFor(
      post(urlPathEqualTo(path))
        .withRequestBody(equalToJson(Json.obj("identifier" -> identifierValue).toString))
        .willReturn(
          aResponse()
            .withStatus(201)
        )
    )

    val result = connector.check(identifierValue).futureValue

    result mustEqual false
  }
