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

import models.YourBankDetails
import models.errors.BarsErrors.*
import models.responses.{Bank, BankAddress, BarsResponse, BarsVerificationResponse, Country}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import connectors.BarsConnector
import config.FrontendAppConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BarsServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: BarsConnector = mock[BarsConnector]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  val service = new BarsService(mockConnector, mockConfig)

  val bank = Bank(
    bankName = "Test Bank",
    address = BankAddress(
      lines = Seq("123 Bank Street", "Suite 100"),
      town = "London",
      country = Country("United Kingdom"),
      postCode = "AB12 3CD"
    )
  )

  val validBankDetails = YourBankDetails(
    sortCode = "123456",
    accountNumber = "12345678",
    accountHolderName = "John Doe"
  )

  "BarsService#barsVerification" should {

    "return Right((BarsVerificationResponse, Bank)) when all checks pass for personal account" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))
      when(mockConnector.getMetadata(any())(any()))
        .thenReturn(Future.successful(Some(bank)))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Right((response, bank))
      }
    }

    "return Left(BankAccountUnverified) when account or name is indeterminate" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Indeterminate,
        nameMatches = BarsResponse.Indeterminate,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(BankAccountUnverified)
      }
    }

    "return Left(AccountDetailInvalidFormat) when account number is badly formatted" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.No,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(AccountDetailInvalidFormat)
      }
    }

    "return Left(SortCodeNotFound) when sort code not present on EISCD" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.No,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(SortCodeNotFound)
      }
    }

    "return Left(SortCodeNotSupported) when sort code does not support direct debit" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.No,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(SortCodeNotSupported)
      }
    }

    "return Left(AccountNotFound) when account does not exist" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.No,
        nameMatches = BarsResponse.Yes,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(AccountNotFound)
      }
    }

    "return Left(NameMismatch) when name does not match" in {
      val response = BarsVerificationResponse(
        accountNumberIsWellFormatted = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD = BarsResponse.Yes,
        sortCodeBankName = Some(bank.bankName),
        accountExists = BarsResponse.Yes,
        nameMatches = BarsResponse.No,
        sortCodeSupportsDirectDebit = BarsResponse.Yes,
        sortCodeSupportsDirectCredit = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = None,
        iban = Some("GB33BUKB20201555555555"),
        accountName = Some("John Doe")
      )

      when(mockConnector.verify(any(), any())(any()))
        .thenReturn(Future.successful(response))

      service.barsVerification("personal", validBankDetails).map { result =>
        result shouldBe Left(NameMismatch)
      }
    }
  }
}
