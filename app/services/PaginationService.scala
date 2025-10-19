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

import models.{DirectDebitDetails, NddDetails}
import viewmodels.govuk.PaginationFluency.*
import utils.Utils.emptyString

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

case class PaginationResult(
  paginatedData: Seq[DirectDebitDetails],
  paginationViewModel: PaginationViewModel,
  totalRecords: Int,
  currentPage: Int,
  totalPages: Int
)

@Singleton
class PaginationService @Inject() {

  val recordsPerPage = 3
  val maxRecords = 99

  def paginateDirectDebits(
    allDirectDebits: Seq[NddDetails],
    currentPage: Int = 1,
    baseUrl: String
  ): PaginationResult = {
    
    val sortedDirectDebits = allDirectDebits
      .sortBy(_.submissionDateTime)(Ordering[LocalDateTime].reverse)
      .take(maxRecords)
    
    val totalRecords = sortedDirectDebits.length
    val totalPages = Math.ceil(totalRecords.toDouble / recordsPerPage).toInt
    val validCurrentPage = Math.max(1, Math.min(currentPage, totalPages))
    
    val startIndex = (validCurrentPage - 1) * recordsPerPage
    val endIndex = Math.min(startIndex + recordsPerPage, totalRecords)
    
    val paginatedData = sortedDirectDebits
      .slice(startIndex, endIndex)
      .map(_.toDirectDebitDetails)
    
    val paginationViewModel = createPaginationViewModel(
      currentPage = validCurrentPage,
      totalPages = totalPages,
      baseUrl = baseUrl
    )
    
    PaginationResult(
      paginatedData = paginatedData,
      paginationViewModel = paginationViewModel,
      totalRecords = totalRecords,
      currentPage = validCurrentPage,
      totalPages = totalPages
    )
  }

  private def createPaginationViewModel(
    currentPage: Int,
    totalPages: Int,
    baseUrl: String
  ): PaginationViewModel = {
    
    if (totalPages <= 1) {
      return PaginationViewModel()
    }
    
    val items = generatePageItems(currentPage, totalPages, baseUrl)
    val previous = if (currentPage > 1) {
      Some(PaginationLinkViewModel(s"$baseUrl?page=${currentPage - 1}").withText("site.pagination.previous"))
    } else None
    
    val next = if (currentPage < totalPages) {
      Some(PaginationLinkViewModel(s"$baseUrl?page=${currentPage + 1}").withText("site.pagination.next"))
    } else None
    
    PaginationViewModel(
      items = items,
      previous = previous,
      next = next
    )
  }

  private def generatePageItems(
    currentPage: Int,
    totalPages: Int,
    baseUrl: String
  ): Seq[PaginationItemViewModel] = {
    
    val maxVisiblePages = 5
    val halfVisible = maxVisiblePages / 2
    
    val startPage = Math.max(1, currentPage - halfVisible)
    val endPage = Math.min(totalPages, startPage + maxVisiblePages - 1)
    
    val adjustedStartPage = if (endPage - startPage < maxVisiblePages - 1) {
      Math.max(1, endPage - maxVisiblePages + 1)
    } else startPage
    
    val pages = (adjustedStartPage to endPage).toList
    
    val items = pages.map { page =>
      PaginationItemViewModel(
        number = page.toString,
        href = s"$baseUrl?page=$page"
      ).withCurrent(page == currentPage)
    }
    
    items
  }
}
