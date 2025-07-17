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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import pages.*
import models.*
import models.DirectDebitSource.{CT, MGD, SA}

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case PersonalOrBusinessAccountPage => _ => routes.YourBankDetailsController.onPageLoad(NormalMode)
    case YourBankDetailsPage => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode)
    case BankDetailsCheckYourAnswerPage => checkBankDetails
    case DirectDebitSourcePage => checkDirectDebitSource
    case _ => _ => routes.IndexController.onPageLoad() // TODO - should redirect to landing controller (when implemented)
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case YourBankDetailsPage => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(CheckMode)
    case BankDetailsCheckYourAnswerPage => checkBankDetails
    case _ => _ => routes.IndexController.onPageLoad() // TODO - should redirect to landing controller (when implemented)
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }

  private def checkBankDetails(userAnswers: UserAnswers): Call =
    userAnswers
      .get(BankDetailsCheckYourAnswerPage)
      .map { isAuthorised =>
        if (isAuthorised) {
          routes.DirectDebitSourceController.onPageLoad(NormalMode)
        } else {
          routes.BankApprovalController.onPageLoad()
        }
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def checkDirectDebitSource(userAnswers: UserAnswers): Call =
    userAnswers
      .get(DirectDebitSourcePage) match {
          case Some(SA) | Some(CT) | Some(MGD) => routes.PaymentPlanTypeController.onPageLoad(NormalMode)
          case _ => routes.PaymentReferenceController.onPageLoad(NormalMode)
        }

}
