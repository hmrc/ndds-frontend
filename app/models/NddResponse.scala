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

package models

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.MaskAndFormatUtils.gdsFormatter

import java.time.{Instant, LocalDateTime}

case class NddDetails(ddiRefNumber: String,
                      submissionDateTime: LocalDateTime,
                      bankSortCode: String,
                      bankAccountNumber: String,
                      bankAccountName: String,
                      auDdisFlag: Boolean,
                      numberOfPayPlans: Int
                     ) {
  val toDirectDebitDetails: DirectDebitDetails = DirectDebitDetails(
    directDebitReference = ddiRefNumber,
    setupDate            = submissionDateTime.format(gdsFormatter),
    sortCode             = bankSortCode,
    accountNumber        = bankAccountNumber,
    paymentPlans         = numberOfPayPlans.toString
  )

  private val encryptString = (encrypter: Encrypter) => (value: String) => encrypter.encrypt(PlainText(value)).value
  private val decryptString = (decrypter: Decrypter) => (value: String) => decrypter.decrypt(Crypted(value)).value

  private def modifyBankDetails(f: String => String): NddDetails = {
    this.copy(
      bankSortCode      = f(this.bankSortCode),
      bankAccountNumber = f(this.bankAccountNumber),
      bankAccountName   = f(this.bankAccountName)
    )
  }

  def encrypted(implicit crypto: Encrypter & Decrypter): NddDetails = modifyBankDetails(encryptString(crypto))
  def decrypted(implicit crypto: Encrypter & Decrypter): NddDetails = modifyBankDetails(decryptString(crypto))
}

object NddDetails {
  implicit val format: OFormat[NddDetails] = Json.format[NddDetails]
}

case class NddResponse(directDebitCount: Int, directDebitList: Seq[NddDetails])

object NddResponse {
  import NddDetails.format
  implicit val format: OFormat[NddResponse] = Json.format[NddResponse]
}

case class NddDAO(id: String, lastUpdated: Instant, directDebits: Seq[NddDetails])

object NddDAO {
  import NddDetails.format
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[NddDAO] = Json.format[NddDAO]
}
