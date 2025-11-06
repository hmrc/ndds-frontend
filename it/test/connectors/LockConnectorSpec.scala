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

import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.{ApplicationWithWiremock, WireMockConstants}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class LockConnectorSpec
  extends AnyWordSpec
    with ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with OptionValues
    with BeforeAndAfterAll
    with IntegrationPatience {


  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy private val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  lazy private val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  lazy private val connector = app.injector.instanceOf[LockConnector]
  

  private val lockResponseJson =
    """{
      |  "_id": "lock-id-1",
      |  "isLocked": true,
      |  "unverifiable": true,
      |  "verifyCalls": 2,
      |  "createdAt": "2024-01-01T12:00:00Z",
      |  "lastUpdated": "2024-01-02T12:00:00Z",
      |  "lockoutExpiryDateTime": "2024-01-10T12:00:00Z"
      |}""".stripMargin

  "LockConnector" should {

    "return LockResponse from checkLock" in {
      val credId = "test-cred-id"
      stubFor(
        post(urlEqualTo("/locks/bars/status"))
          .withRequestBody(equalToJson(s"""{"identifier": "$credId"}"""))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(lockResponseJson)
          )
      )
      val result = connector.checkLock(credId).futureValue
      result._id shouldBe "lock-id-1"
      result.isLocked shouldBe true
      result.unverifiable.value shouldBe true
      result.verifyCalls shouldBe 2
    }

    "return LockResponse from updateLock" in {
      val credId = "test-cred-id"
      stubFor(
        post(urlEqualTo("/locks/bars/update"))
          .withRequestBody(equalToJson(s"""{"identifier": "$credId"}"""))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(lockResponseJson)
          )
      )

      val result = connector.updateLock(credId).futureValue
      result._id shouldBe "lock-id-1"
      result.isLocked shouldBe true
    }

    "return LockResponse from markUnverifiable" in {
      val credId = "test-cred-id"
      stubFor(
        post(urlEqualTo("/locks/bars/markUnverifiable"))
          .withRequestBody(equalToJson(s"""{"identifier": "$credId"}"""))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(lockResponseJson)
          )
      )

      val result = connector.markUnverifiable(credId).futureValue
      result.unverifiable.value shouldBe true
    }

    "fail when service returns error" in {
      val credId = "test-cred-id"
      stubFor(
        post(urlEqualTo("/locks/bars/status"))
          .withRequestBody(equalToJson(s"""{"identifier": "$credId"}"""))
          .willReturn(aResponse().withStatus(500).withBody("error"))
      )

      val ex = intercept[Exception] {
        connector.checkLock(credId).futureValue
      }
      ex.getMessage should include("500")
    }
  }
}