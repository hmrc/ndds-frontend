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

package utils

import models.DirectDebitSource

object Utils {
  val emptyString = ""
  val LockExpirySessionKey = "lockoutExpiryDateTime"

  val listHodServices: Map[DirectDebitSource, String] = Map(
    DirectDebitSource.CT -> "COTA",
    DirectDebitSource.PAYE -> "PAYE",
    DirectDebitSource.SA -> "CESA",
    DirectDebitSource.TC -> "NTC",
    DirectDebitSource.VAT -> "VAT",
    DirectDebitSource.MGD -> "MGD",
    DirectDebitSource.NIC -> "NIDN",
    DirectDebitSource.OL -> "SAFE",
    DirectDebitSource.SDLT -> "SDLT"
  )
  
}

