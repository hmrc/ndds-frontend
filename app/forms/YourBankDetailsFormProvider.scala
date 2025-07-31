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

package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.YourBankDetails

class YourBankDetailsFormProvider @Inject() extends Mappings {

  val MAX_ACCOUNT_HOLDER_NAME_LENGTH = 35
  val MAX_SORT_CODE_LENGTH = 6
  val MAX_ACCOUNT_NUMBER_LENGTH = 8

  def apply(): Form[YourBankDetails] = Form(
    mapping(
      "accountHolderName" -> text("yourBankDetails.error.accountHolderName.required")
        .verifying(maxLength(MAX_ACCOUNT_HOLDER_NAME_LENGTH, "yourBankDetails.error.accountHolderName.length")),
      "sortCode" -> text("yourBankDetails.error.sortCode.required")
        .verifying(firstError(
          minLength(MAX_SORT_CODE_LENGTH, "yourBankDetails.error.sortCode.tooShort"),
          maxLength(MAX_SORT_CODE_LENGTH, "yourBankDetails.error.sortCode.length"),
          regexp(NumericRegex, "yourBankDetails.error.sortCode.numericOnly")
        )),
      "accountNumber" -> text("yourBankDetails.error.accountNumber.required")
        .verifying(firstError(
          minLength(MAX_ACCOUNT_NUMBER_LENGTH, "yourBankDetails.error.accountNumber.tooShort"),
          maxLength(MAX_ACCOUNT_NUMBER_LENGTH, "yourBankDetails.error.accountNumber.length"),
          regexp(NumericRegex, "yourBankDetails.error.accountNumber.numericOnly")
        ))
    )(YourBankDetails.apply)(x => Some((x.accountHolderName, x.sortCode, x.accountNumber)))
  )
}
