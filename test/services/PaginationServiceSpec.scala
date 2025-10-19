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

package services

import base.SpecBase
import models.NddDetails
import org.scalatest.matchers.must.Matchers
import viewmodels.govuk.PaginationFluency.*

import java.time.LocalDateTime

class PaginationServiceSpec extends SpecBase with Matchers {

  val paginationService = new PaginationService()

  "PaginationService" - {

    "paginateDirectDebits" - {

      "must return correct pagination for first page with 3 records per page" in {
        val testData = createTestNddDetails(5)
        val result = paginationService.paginateDirectDebits(testData, currentPage = 1, baseUrl = "/test")

        result.paginatedData.length mustBe 3
        result.currentPage mustBe 1
        result.totalPages mustBe 2
        result.totalRecords mustBe 5
        result.paginationViewModel.previous mustBe None
        result.paginationViewModel.next mustBe defined
      }

      "must return correct pagination for last page" in {
        val testData = createTestNddDetails(5)
        val result = paginationService.paginateDirectDebits(testData, currentPage = 2, baseUrl = "/test")

        result.paginatedData.length mustBe 2
        result.currentPage mustBe 2
        result.totalPages mustBe 2
        result.totalRecords mustBe 5
        result.paginationViewModel.previous mustBe defined
        result.paginationViewModel.next mustBe None
      }

      "must return correct pagination for middle page" in {
        val testData = createTestNddDetails(10)
        val result = paginationService.paginateDirectDebits(testData, currentPage = 2, baseUrl = "/test")

        result.paginatedData.length mustBe 3
        result.currentPage mustBe 2
        result.totalPages mustBe 4
        result.totalRecords mustBe 10
        result.paginationViewModel.previous mustBe defined
        result.paginationViewModel.next mustBe defined
      }

      "must handle empty data" in {
        val result = paginationService.paginateDirectDebits(Seq.empty, baseUrl = "/test")

        result.paginatedData.length mustBe 0
        result.currentPage mustBe 1
        result.totalPages mustBe 0
        result.totalRecords mustBe 0
        result.paginationViewModel.items.length mustBe 0
      }

      "must limit records to maximum of 99" in {
        val testData = createTestNddDetails(150)
        val result = paginationService.paginateDirectDebits(testData, baseUrl = "/test")

        result.totalRecords mustBe 99
        result.totalPages mustBe 33
      }

      "must sort by submission date in descending order (newest first)" in {
        val now = LocalDateTime.now()
        val testData = Seq(
          NddDetails("DD001", now.minusDays(2), "123456", "12345678", "Test Account", false, 1),
          NddDetails("DD002", now.minusDays(1), "123456", "12345678", "Test Account", false, 1),
          NddDetails("DD003", now, "123456", "12345678", "Test Account", false, 1)
        )

        val result = paginationService.paginateDirectDebits(testData, baseUrl = "/test")

        result.paginatedData.head.directDebitReference mustBe "DD003"
        result.paginatedData(1).directDebitReference mustBe "DD002"
        result.paginatedData(2).directDebitReference mustBe "DD001"
      }

      "must handle invalid page numbers gracefully" in {
        val testData = createTestNddDetails(5)

        val resultNegative = paginationService.paginateDirectDebits(testData, currentPage = -1, baseUrl = "/test")
        resultNegative.currentPage mustBe 1

        val resultTooHigh = paginationService.paginateDirectDebits(testData, currentPage = 999, baseUrl = "/test")
        resultTooHigh.currentPage mustBe 2
      }

      "must generate correct pagination links" in {
        val testData = createTestNddDetails(10)
        val result = paginationService.paginateDirectDebits(testData, currentPage = 2, baseUrl = "/test")

        result.paginationViewModel.previous.get.href mustBe "/test?page=1"
        result.paginationViewModel.next.get.href mustBe "/test?page=3"
        result.paginationViewModel.items.exists(_.current) mustBe true
        result.paginationViewModel.items.find(_.current).get.number mustBe "2"
      }

      "must not show pagination when only one page" in {
        val testData = createTestNddDetails(2)
        val result = paginationService.paginateDirectDebits(testData, baseUrl = "/test")

        result.paginationViewModel.items.length mustBe 0
        result.paginationViewModel.previous mustBe None
        result.paginationViewModel.next mustBe None
      }
    }
  }

  private def createTestNddDetails(count: Int): Seq[NddDetails] = {
    val now = LocalDateTime.now()
    (1 to count).map { i =>
      NddDetails(
        ddiRefNumber       = s"DD$i",
        submissionDateTime = now.minusDays(i),
        bankSortCode       = "123456",
        bankAccountNumber  = "12345678",
        bankAccountName    = s"Test Account $i",
        auDdisFlag         = false,
        numberOfPayPlans   = 1
      )
    }
  }
}
