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

package viewmodels

import base.SpecBase
import config.FrontendAppConfig
import connectors.RdsDataCacheConnector
import models.responses.EarliestPaymentDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json

import scala.concurrent.Future

class PaymentDateHelperSpec extends SpecBase {

  val mockConnector = mock[RdsDataCacheConnector]
  val mockConfig = mock[FrontendAppConfig]

  val testHelper = new PaymentDateHelper(mockConnector, mockConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
    reset(mockConfig)
  }


  "PaymentDateHelper" - {
    "getEarliestPaymentDate when" - {
      "must successfully return the Earliest Payment Date" in {
        val expectedUserAnswers = emptyUserAnswers.copy(data =
          Json.obj(
            "yourBankDetails" -> Json.obj(
              "accountHolderName" -> testAccountHolderName,
              "sortCode" -> testSortCode,
              "accountNumber" -> testAccountNumber,
              "auddisStatus" -> true
            )))

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.successful(EarliestPaymentDate("2025-12-25")))

        val result = testHelper.getEarliestPaymentDate(expectedUserAnswers).futureValue

        result mustBe EarliestPaymentDate("2025-12-25")
      }

      "fail when auddis status is not in user answers" in {
        val result = intercept[Exception](testHelper.getEarliestPaymentDate(emptyUserAnswers).futureValue)

        result.getMessage must include("YourBankDetailsPage details missing from user answers")
      }
      "fail when the connector call fails" in {
        val expectedUserAnswers = emptyUserAnswers.copy(data =
          Json.obj(
            "yourBankDetails" -> Json.obj(
              "accountHolderName" -> testAccountHolderName,
              "sortCode" -> testSortCode,
              "accountNumber" -> testAccountNumber,
              "auddisStatus" -> true
            )))

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)
        when(mockConnector.getEarliestPaymentDate(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val result = intercept[Exception](testHelper.getEarliestPaymentDate(expectedUserAnswers).futureValue)

        result.getMessage must include("bang")
      }
    }

    "calculateOffset" - {
      "successfully calculate the offset when auddis status is enabled" in {
        val auddisStatus = true

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisEnabled).thenReturn(3)

        testHelper.calculateOffset(auddisStatus) mustBe "5"
      }
      "successfully calculate the offset when auddis status is not enabled" in {
        val auddisStatus = false
        val expectedVariableDelay = 8

        when(mockConfig.paymentDelayFixed).thenReturn(2)
        when(mockConfig.paymentDelayDynamicAuddisNotEnabled).thenReturn(expectedVariableDelay)

        testHelper.calculateOffset(auddisStatus) mustBe "10"
      }
    }

    "toDateString" - {
      "must convert a local date to the GDS format" in {
        val testModel = EarliestPaymentDate(date = "2025-12-25")

        testHelper.toDateString(testModel) mustBe "25 December 2025"
      }
    }
  }

}
