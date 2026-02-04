/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.validations

import forms.validation.BarsErrorMapper
import models.errors.BarsErrors.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

class BarsErrorMapperSpec extends AnyWordSpec with Matchers {

  "BarsErrorMapper" should {

    "map BankAccountUnverified to correct FormError" in {
      val result = BarsErrorMapper.toFormError(BankAccountUnverified)
      result shouldBe Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified"))
    }

    "map AccountDetailInvalidFormat to correct FormErrors" in {
      val result = BarsErrorMapper.toFormError(AccountDetailInvalidFormat)
      result shouldBe Seq(
        FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
        FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid")
      )
    }

    "map SortCodeNotFound to correct FormErrors" in {
      val result = BarsErrorMapper.toFormError(SortCodeNotFound)
      result shouldBe Seq(
        FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
        FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid")
      )
    }

    "map AccountNotFound to correct FormErrors" in {
      val result = BarsErrorMapper.toFormError(AccountNotFound)
      result shouldBe Seq(
        FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
        FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid")
      )
    }

    "map SortCodeNotSupported to correct FormError" in {
      val result = BarsErrorMapper.toFormError(SortCodeNotSupported)
      result shouldBe Seq(FormError("sortCode", "yourBankDetails.error.sortCodeNotSupported.invalid"))
    }

    "map NameMismatch to correct FormError" in {
      val result = BarsErrorMapper.toFormError(NameMismatch)
      result shouldBe Seq(FormError("accountHolderName", "yourBankDetails.error.nameMismatch.invalid"))
    }

    "map DetailsVerificationFailed to correct FormError" in {
      val result = BarsErrorMapper.toFormError(DetailsVerificationFailed)
      result shouldBe Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified"))
    }
  }
}
