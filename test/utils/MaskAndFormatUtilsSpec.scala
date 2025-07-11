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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class MaskAndFormatUtilsSpec extends AnyFreeSpec with Matchers {

  "MaskAndFormatUtils" - {

    "maskSortCode" - {
      "should mask with first 2 digits and 4 asterisks" in {
        MaskAndFormatUtils.maskSortCode("123456") mustEqual "12****"
      }

      "should handle short sort codes by still adding ****" in {
        MaskAndFormatUtils.maskSortCode("12") mustEqual "12****"
        MaskAndFormatUtils.maskSortCode("1") mustEqual "1****"
        MaskAndFormatUtils.maskSortCode("") mustEqual "****"
      }
    }

    "maskAccountNumber" - {
      "should mask first three digits and show the rest" in {
        MaskAndFormatUtils.maskAccountNumber("12345678") mustEqual "****5678"
      }

      "should mask for short account number" in {
        MaskAndFormatUtils.maskAccountNumber("1234") mustEqual "****"
        MaskAndFormatUtils.maskAccountNumber("123") mustEqual "****"
        MaskAndFormatUtils.maskAccountNumber("12") mustEqual "****"
        MaskAndFormatUtils.maskAccountNumber("") mustEqual "****"
      }

    }

    "formatDateToGds" - {
      "should convert to GDS style date" in {
        MaskAndFormatUtils.formatDateToGds("5/7/2025") mustEqual "5 July 2025"
        MaskAndFormatUtils.formatDateToGds("15/12/2023") mustEqual "15 December 2023"
      }
    }
  }
}
