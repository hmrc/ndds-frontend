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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import models.YourBankDetails
import models.responses.{BarsVerificationResponse, Country}
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

class BarsConnectorSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with OptionValues {

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
    sortCode = "443116",
    accountNumber = "55207102"
  )

  private val personalRequestJson = Json.obj(
    "account" -> Json.obj(
      "sortCode" -> bankDetails.sortCode,
      "accountNumber" -> bankDetails.accountNumber
    ),
    "name" -> bankDetails.accountHolderName
  )

  private val businessRequestJson = Json.obj(
    "account" -> Json.obj(
      "sortCode" -> bankDetails.sortCode,
      "accountNumber" -> bankDetails.accountNumber
    ),
    "businessName" -> bankDetails.accountHolderName
  )

  "BarsConnector.verify" should {

    "return verification result with metadata when personal endpoint succeeds" in {
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

      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """{
                  "bankName": "Some Bank",
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

      val result = connector.verify("personal", personalRequestJson).futureValue

      result.accountNumberIsWellFormatted.toString.toLowerCase shouldBe "yes"
      result.bank.value.bankName shouldBe "Some Bank"
      result.bank.value.address.lines shouldEqual Seq("123 Bank Street")
      result.bank.value.address.town shouldBe "London"
      result.bank.value.address.country shouldBe Country("United Kingdom")
      result.bank.value.address.postCode shouldBe "SW1A 1AA"
    }

    "return verification result without bank metadata when metadata endpoint returns 404" in {
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

      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(aResponse().withStatus(404))
      )

      val result = connector.verify("personal", personalRequestJson).futureValue
      result.bank shouldBe None
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

    "successfully verify business bank details and fetch metadata" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/verify/business"))
          .withRequestBody(equalToJson(businessRequestJson.toString()))
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

      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """{
                  "bankName": "Some Business Bank",
                  "address": {
                    "lines": ["456 Business Road"],
                    "town": "Manchester",
                    "country": { "name": "United Kingdom" },
                    "postCode": "M1 2AB"
                  }
                }"""
              )
          )
      )

      val result = connector.verify("business", businessRequestJson).futureValue

      result.accountNumberIsWellFormatted.toString.toLowerCase shouldBe "yes"
      result.bank.value.bankName shouldBe "Some Business Bank"
      result.bank.value.address.lines shouldEqual Seq("456 Business Road")
      result.bank.value.address.town shouldBe "Manchester"
      result.bank.value.address.country shouldBe Country("United Kingdom")
      result.bank.value.address.postCode shouldBe "M1 2AB"
    }

    "fail when metadata endpoint responds with 500" in {
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
      wireMockServer.stubFor(
        get(urlEqualTo("/metadata/443116"))
          .willReturn(aResponse().withStatus(500))
      )
      val ex = intercept[Exception] {
        connector.verify("personal", personalRequestJson).futureValue
      }
      ex.getMessage should include("500")
    }
  }
}
