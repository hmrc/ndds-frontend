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

import config.CurrencyFormatter.currencyFormat
import config.FrontendAppConfig
import forms.behaviours.CurrencyFieldBehaviours
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class TotalAmountDueFormProviderSpec extends CurrencyFieldBehaviours with MockitoSugar {

  val mockConfig = mock[FrontendAppConfig]
  when(mockConfig.minimumLiabilityAmount).thenReturn(BigDecimal("12.00"))
  
  val form = new TotalAmountDueFormProvider(mockConfig)()

  ".value" - {

    val fieldName = "value"

    val minimum = 12
    val maximum = 999999

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "totalAmountDue.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "totalAmountDue.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "totalAmountDue.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like currencyFieldWithMinimum(
      form,
      fieldName,
      minimum,
      FormError(fieldName, "totalAmountDue.error.belowMinimum", Seq(currencyFormat(minimum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "totalAmountDue.error.required")
    )

    "must validate the minimum liability amount from config" in {
      val testConfig = mock[FrontendAppConfig]
      when(testConfig.minimumLiabilityAmount).thenReturn(BigDecimal("25.00"))
      
      val testForm = new TotalAmountDueFormProvider(testConfig)()
      
      val result = testForm.bind(Map("value" -> "10.00"))
      
      result.errors must contain(FormError("value", "totalAmountDue.error.belowMinimum", Seq("£25")))
    }
  }
}
