/*
 * Copyright 2024 HM Revenue & Customs
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

import models.DirectDebitDetails


trait DirectDebitDetailsData {

  val directDebitDetailsData: List[DirectDebitDetails] = List(DirectDebitDetails(
    directDebitReference ="122222",
    setupDate="01/02/2024",
    sortCode = "666666",
    accountNumber = "00000000",
    paymentPlans = "0"
  ), DirectDebitDetails(
    directDebitReference ="133333",
    setupDate="02/03/2024",
    sortCode = "555555",
    accountNumber = "11111111",
    paymentPlans = "0"
  ),
    DirectDebitDetails(
      directDebitReference ="144444",
      setupDate="03/03/2024",
      sortCode = "333333",
      accountNumber = "22222222",
      paymentPlans = "0"
      
    ))
}

