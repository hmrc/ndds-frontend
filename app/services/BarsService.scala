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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.BarsConnector
import models.YourBankDetails
import models.errors.BarsErrors
import models.errors.BarsErrors.*
import models.requests.{BarsAccount, BarsBusiness, BarsBusinessRequest, BarsPersonalRequest, BarsSubject}
import models.responses.{BarsResponse, BarsVerificationResponse, Bank}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class BarsService @Inject()(
                                  barsConnector: BarsConnector,
                                  config: FrontendAppConfig
                                )(implicit ec: ExecutionContext) {

  // --- Validation helpers ---
  private val checkAccountAndName = (accountExists: BarsResponse, nameMatches: BarsResponse) => {
    if (accountExists == BarsResponse.Indeterminate || nameMatches == BarsResponse.Indeterminate)
      Left(BankAccountUnverified)
    else Right(())
  }

  private val checkAccountNumberFormat = (accountNumberIsWellFormatted: BarsResponse) =>
    if (accountNumberIsWellFormatted == BarsResponse.No) Left(AccountDetailInvalidFormat) else Right(())

  private val checkSortCodeExistsOnEiscd = (sortCodeIsPresentOnEISCD: BarsResponse) =>
    if (sortCodeIsPresentOnEISCD == BarsResponse.No) Left(SortCodeNotFound) else Right(())

  private val checkSortCodeDirectDebitSupport = (sortCodeSupportsDirectDebit: BarsResponse) =>
    if (sortCodeSupportsDirectDebit == BarsResponse.No) Left(SortCodeNotSupported) else Right(())

  private val checkAccountExists = (accountExists: BarsResponse) =>
    if (accountExists == BarsResponse.No || accountExists == BarsResponse.Inapplicable) Left(AccountNotFound) else Right(())

  private val checkNameMatches = (nameMatches: BarsResponse, accountExists: BarsResponse) =>
    if (nameMatches == BarsResponse.No || nameMatches == BarsResponse.Inapplicable ||
      (nameMatches == BarsResponse.Indeterminate && accountExists != BarsResponse.Indeterminate)) Left(NameMismatch)
    else Right(())

  private val checkBarsResponseSuccess = (response: BarsVerificationResponse) =>
    (response.accountNumberIsWellFormatted == BarsResponse.Yes || response.accountNumberIsWellFormatted == BarsResponse.Indeterminate) &&
      response.sortCodeIsPresentOnEISCD == BarsResponse.Yes &&
      response.accountExists == BarsResponse.Yes &&
      (response.nameMatches == BarsResponse.Yes || response.nameMatches == BarsResponse.Partial) &&
      response.sortCodeSupportsDirectDebit == BarsResponse.Yes

  // --- Main verification method ---
  def barsVerification(personalOrBusiness: String, bankDetails: YourBankDetails)
                      (implicit hc: HeaderCarrier): Future[Either[BarsErrors, (BarsVerificationResponse, Bank)]] = {

    val (endpoint, requestJson) = if (personalOrBusiness.toLowerCase == "personal") {
      "personal" -> Json.toJson(
        BarsPersonalRequest(
          BarsAccount(bankDetails.sortCodeNoSpaces, bankDetails.accountNumber),
          BarsSubject(bankDetails.accountHolderName)
        )
      )
    } else {
      "business" -> Json.toJson(
        BarsBusinessRequest(
          BarsAccount(bankDetails.sortCodeNoSpaces, bankDetails.accountNumber),
          BarsBusiness(bankDetails.accountHolderName)
        )
      )
    }

    for {
      verificationResponse <- barsConnector.verify(endpoint, requestJson)
      result <- if (checkBarsResponseSuccess(verificationResponse)) {
        // Only call getMetadata if verification succeeded
        barsConnector.getMetadata(bankDetails.sortCodeNoSpaces).map { bank =>
          Right((verificationResponse, bank))
        }
      } else {
        val validatedResult: Either[BarsErrors, Unit] = for {
          _ <- checkAccountAndName(verificationResponse.accountExists, verificationResponse.nameMatches)
          _ <- checkAccountNumberFormat(verificationResponse.accountNumberIsWellFormatted)
          _ <- checkSortCodeExistsOnEiscd(verificationResponse.sortCodeIsPresentOnEISCD)
          _ <- checkSortCodeDirectDebitSupport(verificationResponse.sortCodeSupportsDirectDebit)
          _ <- checkAccountExists(verificationResponse.accountExists)
          _ <- checkNameMatches(verificationResponse.nameMatches, verificationResponse.accountExists)
        } yield Right(())

        Future.successful(Left(validatedResult.fold(identity, _ => DetailsVerificationFailed)))
      }
    } yield result
  }
}
