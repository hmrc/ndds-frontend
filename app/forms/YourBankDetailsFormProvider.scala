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
import utils.Constants._

class YourBankDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[YourBankDetails] = Form(
    mapping(
      "accountHolderName" -> text("yourBankDetails.error.accountHolderName.required")
        .verifying(maxLength(MaxAccountHolderNameLength, "yourBankDetails.error.accountHolderName.length")),
      "sortCode" -> text("yourBankDetails.error.sortCode.required")
        .verifying(firstError(
          minLength(MaxSortCodeLength, "yourBankDetails.error.sortCode.tooShort"),
          maxLength(MaxSortCodeLength, "yourBankDetails.error.sortCode.length"),
          regexp(NumericRegex, "yourBankDetails.error.sortCode.numericOnly")
        )),
      "accountNumber" -> text("yourBankDetails.error.accountNumber.required")
        .verifying(firstError(
          minLength(MaxAccountNumberLength, "yourBankDetails.error.accountNumber.tooShort"),
          maxLength(MaxAccountNumberLength, "yourBankDetails.error.accountNumber.length"),
          regexp(NumericRegex, "yourBankDetails.error.accountNumber.numericOnly")
        ))
    )(YourBankDetails.apply)(x => Some((x.accountHolderName, x.sortCode, x.accountNumber)))
  )
}
