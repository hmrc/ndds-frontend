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

import config.FrontendAppConfig
import connectors.BARSConnector
import models.YourBankDetails
import models.errors.BarsErrors.*
import models.responses.BarsResponse.*
import models.responses.{BarsResponse, BarsVerificationResponse}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BARServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockConnector: BARSConnector = mock[BARSConnector]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val service = BARService(mockConnector, mockConfig)

  val sampleBankDetails = YourBankDetails(
    accountNumber = "12345678",
    sortCode = "123456",
    accountHolderName = "John Doe"
  )

  def createResponse(
                      accountNumberIsWellFormatted: BarsResponse,
                      sortCodeIsPresentOnEISCD: BarsResponse,
                      accountExists: BarsResponse,
                      nameMatches: BarsResponse,
                      sortCodeSupportsDirectDebit: BarsResponse
                    ): BarsVerificationResponse = BarsVerificationResponse(
    accountNumberIsWellFormatted = accountNumberIsWellFormatted,
    sortCodeIsPresentOnEISCD = sortCodeIsPresentOnEISCD,
    sortCodeBankName = None,
    accountExists = accountExists,
    nameMatches = nameMatches,
    sortCodeSupportsDirectDebit = sortCodeSupportsDirectDebit,
    sortCodeSupportsDirectCredit = Yes,
    nonStandardAccountDetailsRequiredForBacs = None,
    iban = None,
    accountName = None,
    bank = None
  )

  "BARService#barsVerification" should {

    "return Right(response) when all BARS checks pass" in {
      val response = createResponse(Yes, Yes, Yes, Yes, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Right(response)
      }
    }

    "return Left(AccountDetailInvalidFormat) if account number is badly formatted" in {
      val response = createResponse(No, Yes, Yes, Yes, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(AccountDetailInvalidFormat)
      }
    }

    "return Left(SortCodeNotFound) if sort code does not exist on EISCD" in {
      val response = createResponse(Yes, No, Yes, Yes, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(SortCodeNotFound)
      }
    }

    "return Left(NameMismatch) if name does not match" in {
      val response = createResponse(Yes, Yes, Yes, No, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(NameMismatch)
      }
    }

    "return Left(BankAccountUnverified) if accountExists or nameMatches are Indeterminate" in {
      val response = createResponse(Yes, Yes, Indeterminate, Yes, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(BankAccountUnverified)
      }
    }

    "return Left(SortCodeNotSupported) if sort code does not support direct debit" in {
      val response = createResponse(Yes, Yes, Yes, Yes, No)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(SortCodeNotSupported)
      }
    }

    "return Left(AccountNotFound) if account does not exist" in {
      val response = createResponse(Yes, Yes, No, Yes, Yes)

      when(mockConnector.verify(true, sampleBankDetails)).thenReturn(Future.successful(response))

      service.barsVerification("personal", sampleBankDetails).map { result =>
        result shouldBe Left(AccountNotFound)
      }
    }

  }
}
