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

package forms.validation

import models.errors.BarsErrors
import models.errors.BarsErrors.*
import play.api.data.{Form, FormError}
import play.api.data.Forms.*

object BarsErrorMapper {

  def toFormError(error: BarsErrors): Seq[FormError] = {
    error match {
      case BankAccountUnverified =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified")
        )
      case AccountDetailInvalidFormat =>
        Seq(FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
          FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid"))
      case SortCodeNotFound =>
        Seq(FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
          FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid"))
      case SortCodeNotSupported =>
        Seq(
          FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid"))
      case AccountNotFound =>
        Seq(FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
          FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid"))
      case NameMismatch =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.accountNumberAndSortCode.invalid")
        )
      case DetailsVerificationFailed =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified")
        )

    }
  }
}
