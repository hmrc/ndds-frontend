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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import models.YourBankDetails
import models.responses.{Bank, BarsResponse, BarsVerificationResponse, Country}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

class BarsConnectorSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterAll with OptionValues {

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val hc: HeaderCarrier = HeaderCarrier()

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  override def beforeAll(): Unit = {
    wireMockServer.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  lazy private val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.bars.host" -> "localhost",
      "microservice.services.bars.port" -> wireMockServer.port()
    )
    .build()

  lazy private val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  lazy private val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  lazy private val connector = BarsConnector(config, httpClient)

  private val bankDetails = YourBankDetails(
    accountHolderName = "Teddy Dickson",
    sortCode          = "443116",
    accountNumber     = "55207102"
  )

  private val personalRequestJson = Json.obj(
    "account" -> Json.obj(
      "sortCode"      -> bankDetails.sortCode,
      "accountNumber" -> bankDetails.accountNumber
    ),
    "name" -> bankDetails.accountHolderName
  )

  "BarsConnector.verify" should {

    "return verification result when personal endpoint succeeds" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/verify/personal"))
          .withRequestBody(equalToJson(personalRequestJson.toString()))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """{
                  "accountNumberIsWellFormatted": "yes",
                  "sortCodeIsPresentOnEISCD": "yes",
                  "accountExists": "yes",
                  "nameMatches": "yes",
                  "sortCodeSupportsDirectDebit": "yes",
                  "sortCodeSupportsDirectCredit": "yes"
                }"""
              )
          )
      )

      val result = connector.verify("personal", personalRequestJson).futureValue
      result.accountNumberIsWellFormatted shouldBe BarsResponse.Yes
      result.sortCodeIsPresentOnEISCD     shouldBe BarsResponse.Yes
      result.accountExists                shouldBe BarsResponse.Yes
      result.nameMatches                  shouldBe BarsResponse.Yes
    }

    "fail when verification endpoint returns 500" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/verify/personal"))
          .withRequestBody(equalToJson(personalRequestJson.toString()))
          .willReturn(aResponse().withStatus(500))
      )

      val ex = intercept[Exception] {
        connector.verify("personal", personalRequestJson).futureValue
      }
      ex.getMessage should include("500")
    }
  }

  "BarsConnector.getMetadata" should {

    "return bank metadata when metadata endpoint succeeds" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """{
                  "bankName": "Some Bank",
                  "ddiVoucherFlag": "Y",
                  "address": {
                    "lines": ["123 Bank Street"],
                    "town": "London",
                    "country": { "name": "United Kingdom" },
                    "postCode": "SW1A 1AA"
                  }
                }"""
              )
          )
      )

      val result = connector.getMetadata("443116").futureValue
      result.bankName         shouldBe "Some Bank"
      result.address.lines shouldEqual Seq("123 Bank Street")
      result.address.town     shouldBe Some("London")
      result.address.country  shouldBe Country("United Kingdom")
      result.address.postCode shouldBe Some("SW1A 1AA")
    }

    "fail when metadata endpoint responds with 500" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(aResponse().withStatus(500))
      )

      val ex = intercept[Exception] {
        connector.getMetadata("443116").futureValue
      }
      ex.getMessage should include("500")
    }
  }
}
