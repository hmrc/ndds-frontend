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

package forms

import forms.mappings.Mappings
import models.YourBankDetails
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class YourBankDetailsFormProvider @Inject() extends Mappings {

  val MAX_ACCOUNT_HOLDER_NAME_LENGTH = 35
  val MAX_SORT_CODE_LENGTH = 6
  val MAX_ACCOUNT_NUMBER_LENGTH = 8

  private def normaliseSortCode(value: String): String =
    value.replaceAll("[\\s-]", "")

  private val sortCodeConstraint: Constraint[String] =
    Constraint("constraints.sortCode") { value =>
      val normalised = normaliseSortCode(value)

      if (normalised.length < MAX_SORT_CODE_LENGTH) {
        Invalid("yourBankDetails.error.sortCode.tooShort", MAX_SORT_CODE_LENGTH)
      } else if (normalised.length > MAX_SORT_CODE_LENGTH) {
        Invalid("yourBankDetails.error.sortCode.length", MAX_SORT_CODE_LENGTH)
      } else if (!normalised.forall(_.isDigit)) {
        Invalid("yourBankDetails.error.sortCode.numericOnly")
      } else {
        Valid
      }
    }

  def apply(): Form[YourBankDetails] = Form(
    mapping(
      "accountHolderName" -> text("yourBankDetails.error.accountHolderName.required")
        .verifying(maxLength(MAX_ACCOUNT_HOLDER_NAME_LENGTH, "yourBankDetails.error.accountHolderName.length")),
      "sortCode" -> text("yourBankDetails.error.sortCode.required")
        .verifying(sortCodeConstraint),
      "accountNumber" -> text("yourBankDetails.error.accountNumber.required")
        .transform[String](value => value.replaceAll("\\s", ""), identity)
        .verifying(
          firstError(
            minLength(MAX_ACCOUNT_NUMBER_LENGTH, "yourBankDetails.error.accountNumber.tooShort"),
            maxLength(MAX_ACCOUNT_NUMBER_LENGTH, "yourBankDetails.error.accountNumber.length"),
            regexp(NumericRegex, "yourBankDetails.error.accountNumber.numericOnly")
          )
        )
    )(YourBankDetails(_, _, _))(x => Some((x.accountHolderName, x.sortCode, x.accountNumber)))
  )
}
