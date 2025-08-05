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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.MaskAndFormatUtils.gdsFormatter

import java.time.{Instant, LocalDateTime}

case class RDSDirectDebitDetails(ddiRefNumber: String,
                                 submissionDateTime: LocalDateTime,
                                 bankSortCode: String,
                                 bankAccountNumber: String,
                                 bankAccountName: String,
                                 auDdisFlag: Boolean,
                                 numberOfPayPlans: Int) {
  val toDirectDebitDetails: DirectDebitDetails = DirectDebitDetails(
    directDebitReference = ddiRefNumber,
    setupDate = submissionDateTime.format(gdsFormatter),
    sortCode = bankSortCode,
    accountNumber = bankAccountNumber,
    paymentPlans = numberOfPayPlans.toString
  )
}

object RDSDirectDebitDetails {
  implicit val format: OFormat[RDSDirectDebitDetails] = Json.format[RDSDirectDebitDetails]
  java.time.Month.values()
}


case class RDSDatacacheResponse(directDebitCount: Int,
                                directDebitList: Seq[RDSDirectDebitDetails])

object RDSDatacacheResponse {
  import RDSDirectDebitDetails.format
  implicit val format: OFormat[RDSDatacacheResponse] = Json.format[RDSDatacacheResponse]
}


case class RDSDatacacheDAO(id: String,
                           lastUpdated: Instant,
                           directDebits: Seq[RDSDirectDebitDetails])

object RDSDatacacheDAO {
  import RDSDirectDebitDetails.format
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[RDSDatacacheDAO] = Json.format[RDSDatacacheDAO]
}