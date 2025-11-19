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
import play.api.data.FormError

object BarsErrorMapper {

  def toFormError(error: BarsErrors): Seq[FormError] = {
    error match {
      case SortCodeOnDenyList => Seq(FormError("sortCode", "yourBankDetails.error.sortCodeOnDenyList"))
      case BankAccountUnverified =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified"))

      case AccountDetailInvalidFormat | SortCodeNotFound | AccountNotFound =>
        Seq(
          FormError("accountNumber", "yourBankDetails.error.accountNumberAndSortCode.invalid"),
          FormError("sortCode", "yourBankDetails.error.accountNumberAndSortCode.invalid")
        )

      case SortCodeNotSupported =>
        Seq(FormError("sortCode", "yourBankDetails.error.sortCodeNotSupported.invalid"))

      case NameMismatch =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.nameMismatch.invalid"))

      case DetailsVerificationFailed =>
        Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified"))

      case null => Seq(FormError("accountHolderName", "yourBankDetails.error.accountHolderName.unverified"))

    }
  }
}
