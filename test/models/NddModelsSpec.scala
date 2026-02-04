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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.{Instant, LocalDateTime}

class NddModelsSpec extends AnyWordSpec with Matchers {

  private val sampleDateTime = LocalDateTime.of(2025, 3, 15, 10, 45, 30)
  private val sampleInstant = Instant.parse("2025-03-15T10:45:30Z")

  "NddDetails" should {

    "serialize and deserialize to JSON correctly" in {
      val details = NddDetails(
        ddiRefNumber       = "DDI123",
        submissionDateTime = sampleDateTime,
        bankSortCode       = "123456",
        bankAccountNumber  = "12345678",
        bankAccountName    = "John Doe",
        auDdisFlag         = true,
        numberOfPayPlans   = 3
      )

      val json = Json.toJson(details)
      (json \ "ddiRefNumber").as[String]      shouldBe "DDI123"
      (json \ "bankSortCode").as[String]      shouldBe "123456"
      (json \ "bankAccountNumber").as[String] shouldBe "12345678"
      (json \ "bankAccountName").as[String]   shouldBe "John Doe"
      (json \ "auDdisFlag").as[Boolean]       shouldBe true
      (json \ "numberOfPayPlans").as[Int]     shouldBe 3

      val parsed = json.as[NddDetails]
      parsed shouldBe details
    }

    "convert to DirectDebitDetails correctly" in {
      val details = NddDetails(
        ddiRefNumber       = "DDI456",
        submissionDateTime = sampleDateTime,
        bankSortCode       = "654321",
        bankAccountNumber  = "87654321",
        bankAccountName    = "Jane Smith",
        auDdisFlag         = false,
        numberOfPayPlans   = 2
      )

      val directDebitDetails = details.toDirectDebitDetails

      directDebitDetails.directDebitReference shouldBe "DDI456"
      directDebitDetails.sortCode             shouldBe "654321"
      directDebitDetails.accountNumber        shouldBe "87654321"
      directDebitDetails.paymentPlans         shouldBe "2"

      val expectedDate = details.submissionDateTime.format(utils.MaskAndFormatUtils.gdsShortMonthFormatter)
      directDebitDetails.setupDate shouldBe expectedDate
    }
  }

  "NddResponse" should {

    "serialize and deserialize with nested NddDetails" in {
      val details = NddDetails(
        ddiRefNumber       = "DDI789",
        submissionDateTime = sampleDateTime,
        bankSortCode       = "111222",
        bankAccountNumber  = "33344455",
        bankAccountName    = "Alice Example",
        auDdisFlag         = true,
        numberOfPayPlans   = 1
      )

      val response = NddResponse(directDebitCount = 1, directDebitList = Seq(details))

      val json = Json.toJson(response)

      (json \ "directDebitCount").as[Int]                        shouldBe 1
      (json \ "directDebitList")(0).\("ddiRefNumber").as[String] shouldBe "DDI789"

      val parsed = json.as[NddResponse]
      parsed shouldBe response
    }
  }

  "NddDAO" should {

    "serialize and deserialize with Instant and nested NddDetails" in {
      val details = NddDetails(
        ddiRefNumber       = "DDI999",
        submissionDateTime = sampleDateTime,
        bankSortCode       = "999000",
        bankAccountNumber  = "12312312",
        bankAccountName    = "Bob Example",
        auDdisFlag         = false,
        numberOfPayPlans   = 4
      )

      val dao = NddDAO(
        id           = "someId",
        lastUpdated  = sampleInstant,
        directDebits = Seq(details)
      )

      val json = Json.toJson(dao)

      (json \ "id").as[String]                                shouldBe "someId"
      (json \ "directDebits")(0).\("ddiRefNumber").as[String] shouldBe "DDI999"

      val parsed = json.as[NddDAO]
      parsed             shouldBe dao
      parsed.lastUpdated shouldBe sampleInstant
    }
  }

}
