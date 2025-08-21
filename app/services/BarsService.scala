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
import models.responses.BarsResponse.*
import models.responses.{BarsResponse, BarsVerificationResponse}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class BarsService @Inject()(
                                 barsConnector: BarsConnector,
                                 config: FrontendAppConfig
                               )(implicit ec: ExecutionContext) {

  private val checkAccountAndName = (accountExists: BarsResponse, nameMatches: BarsResponse) => {
    if (accountExists == Indeterminate || nameMatches == Indeterminate) {
      Left(BankAccountUnverified)
    } else {
      Right(())
    }
  }

  private val checkAccountNumberFormat = (accountNumberIsWellFormatted: BarsResponse) =>
    if (accountNumberIsWellFormatted == No) Left(AccountDetailInvalidFormat) else Right(())

  private val checkSortCodeExistsOnEiscd = (sortCodeIsPresentOnEiscd: BarsResponse) =>
    if (sortCodeIsPresentOnEiscd == No) Left(SortCodeNotFound) else Right(())

  private val checkSortCodeDirectDebitSupport = (sortCodeSupportsDirectDebit: BarsResponse) =>
    if (sortCodeSupportsDirectDebit == No) Left(SortCodeNotSupported) else Right(())

  private val checkAccountExists = (accountExists: BarsResponse) =>
    if (accountExists == No || accountExists == Inapplicable) Left(AccountNotFound) else Right(())

  private val checkNameMatches = (nameMatches: BarsResponse, accountExists: BarsResponse) =>
    if (nameMatches == No || nameMatches == Inapplicable || (nameMatches == Indeterminate && accountExists != Indeterminate)) Left(NameMismatch) else Right(())

  private val checkBarsResponseSuccess = (response: BarsVerificationResponse) => {
    (response.accountNumberIsWellFormatted == Yes || response.accountNumberIsWellFormatted == Indeterminate) &&
      response.sortCodeIsPresentOnEISCD == Yes &&
      response.accountExists == Yes &&
      (response.nameMatches == Yes || response.nameMatches == Partial) &&
      response.sortCodeSupportsDirectDebit == Yes
  }

  def barsVerification(personalOrBusiness: String, bankDetails: YourBankDetails)
                      (implicit hc: HeaderCarrier): Future[Either[BarsErrors, BarsVerificationResponse]] = {
    val (verifyUrl, requestJson) = if (personalOrBusiness.toLowerCase == "personal") {
      ("personal", Json.toJson(
        BarsPersonalRequest(
          BarsAccount(bankDetails.sortCode, bankDetails.accountNumber),
          BarsSubject(bankDetails.accountHolderName)
        )
      ))
    } else {
      ("business", Json.toJson(
        BarsBusinessRequest(
          BarsAccount(bankDetails.sortCode, bankDetails.accountNumber),
          BarsBusiness(bankDetails.accountHolderName)
        )
      ))
    }

    barsConnector.verify(verifyUrl, requestJson).map { response =>
      if (checkBarsResponseSuccess(response)) {
        Right(response)
      } else {
        val validatedResult: Either[BarsErrors, Unit] = for {
          _ <- checkAccountAndName(response.accountExists, response.nameMatches)
          _ <- checkAccountNumberFormat(response.accountNumberIsWellFormatted)
          _ <- checkSortCodeExistsOnEiscd(response.sortCodeIsPresentOnEISCD)
          _ <- checkSortCodeDirectDebitSupport(response.sortCodeSupportsDirectDebit)
          _ <- checkAccountExists(response.accountExists)
          _ <- checkNameMatches(response.nameMatches, response.accountExists)
        } yield Right(())

        Left(validatedResult.fold(identity, _ => DetailsVerificationFailed))
      }
    }
  }

}
