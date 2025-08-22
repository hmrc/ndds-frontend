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

import base.SpecBase

class YourBankDetailsWithAuddisStatusSpec extends SpecBase {

  "YourBankDetailsWithAuddisStatus" - {
    val testSortCode = "123456"
    val testAccountNumber = "12345678"
    val testAccountHolderName = "Jon B Jones"

    "toModelWithAuddisStatus method" - {
      "must successfully convert from YourBankDetails to YourBankDetailsWithAuddisStatus" in {
        val testAuddisStatus = true
        val testModel = YourBankDetails(accountHolderName = testAccountHolderName, sortCode = testSortCode, accountNumber = testAccountNumber)

        val expectedResult = YourBankDetailsWithAuddisStatus(accountHolderName = testAccountHolderName,
          sortCode = testSortCode, accountNumber = testAccountNumber, auddisStatus = true, false)
        val result = YourBankDetailsWithAuddisStatus.toModelWithAuddisStatus(testModel, testAuddisStatus, false)

        result mustEqual expectedResult
      }
    }

    "toModelWithAuddisStatus method" - {
      "must successfully convert from YourBankDetailsWithAuddisStatus to YourBankDetails" in {
        val testModel = YourBankDetailsWithAuddisStatus(accountHolderName = testAccountHolderName,
          sortCode = testSortCode, accountNumber = testAccountNumber, auddisStatus = true, false)

        val expectedResult = YourBankDetails(accountHolderName = testAccountHolderName,
          sortCode = testSortCode, accountNumber = testAccountNumber)
        val result = YourBankDetailsWithAuddisStatus.toModelWithoutAuddisStatus(testModel)

        result mustEqual expectedResult
      }
    }
  }

}
