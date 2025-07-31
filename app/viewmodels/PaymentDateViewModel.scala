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
import models.requests.WorkingDaysOffsetRequest
import models.responses.EarliestPaymentDate
import models.{Mode, UserAnswers}
import pages.YourBankDetailsPage
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.Future

case class PaymentDateViewModel(mode: Mode, earliestPaymentDate: String)

class PaymentDateHelper @Inject()(
                                    rdsDataCacheConnector: RdsDataCacheConnector,
                                    frontendAppConfig: FrontendAppConfig
                                    ) {

  def getEarliestPaymentDate(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[EarliestPaymentDate] = {
    val auddisStatus = userAnswers.get(YourBankDetailsPage).map(_.auddisStatus)
      .getOrElse(throw new Exception("YourBankDetailsPage details missing from user answers"))
    val offsetWorkingDays = calculateOffset(auddisStatus)
    val currentDate = LocalDate.now().toString
    
    rdsDataCacheConnector.getEarliestPaymentDate(WorkingDaysOffsetRequest(baseDate = currentDate, offsetWorkingDays = offsetWorkingDays))
  }
  
  private[viewmodels] def calculateOffset(auddisStatus: Boolean): Int = {
    val fixedDelay = frontendAppConfig.paymentDelayFixed

    val dynamicDelay = if (auddisStatus) {
      frontendAppConfig.paymentDelayDynamicAuddisEnabled
    } else {
      frontendAppConfig.paymentDelayDynamicAuddisNotEnabled
    }

    val totalDelay = fixedDelay + dynamicDelay

    totalDelay
  }
  
  def toDateString(earliestPaymentDate: EarliestPaymentDate): String = {
    val date = LocalDate.parse(earliestPaymentDate.date)
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    
    date.format(formatter)
  }
  
}