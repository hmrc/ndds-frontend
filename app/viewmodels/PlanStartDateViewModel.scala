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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RdsDataCacheConnector
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.{BudgetPaymentPlan, TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.requests.WorkingDaysOffsetRequest
import models.responses.EarliestPaymentDate
import models.{DirectDebitSource, Mode, PaymentPlanType, UserAnswers}
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, YourBankDetailsPage}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import scala.concurrent.Future

case class PlanStartDateViewModel(mode: Mode)

class PlanStartDateHelper @Inject()(
                                     rdsDataCacheConnector: RdsDataCacheConnector,
                                     frontendAppConfig: FrontendAppConfig
                                   ) {

  def getEarliestPlanStartDate(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers.get(YourBankDetailsPage).map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val paymentPlanType = userAnswers.get(PaymentPlanTypePage)
      .getOrElse(throw new Exception("PaymentPlanTypePage details missing from user answers"))
    val directDebitSource = userAnswers.get(DirectDebitSourcePage)
      .getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers"))

    val offsetWorkingDays = calculateOffset(auddisStatus, paymentPlanType, directDebitSource)
    val currentDate = LocalDate.now().toString

    rdsDataCacheConnector.getEarliestPaymentDate(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
  }

  private[viewmodels] def calculateOffset(auddisStatus: Boolean, paymentPlanType: PaymentPlanType, directDebitSource: DirectDebitSource): Int = {
    (paymentPlanType, directDebitSource) match {
      case (VariablePaymentPlan, MGD) => frontendAppConfig.variableMgdFixedDelay
      case (BudgetPaymentPlan, SA) | (TaxCreditRepaymentPlan, TC) =>
        val fixedDelay = frontendAppConfig.paymentDelayFixed
        val dynamicDelay = if (auddisStatus) {
          frontendAppConfig.paymentDelayDynamicAuddisEnabled
        } else {
          frontendAppConfig.paymentDelayDynamicAuddisNotEnabled
        }
        val totalDelay = fixedDelay + dynamicDelay
        totalDelay
      case _ => throw new InternalServerException("User should not be on this page without being on one of the specified journeys")
    }
  }
}

