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

import base.SpecBase
import controllers.routes
import pages.*
import models.*

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator
  val userAnswers: UserAnswers = UserAnswers("id")

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "must go from a PersonalOrBusinessAccountPage to YourBankDetailsPage" in {
        navigator.nextPage(PersonalOrBusinessAccountPage, NormalMode, userAnswers) mustBe routes.YourBankDetailsController.onPageLoad(NormalMode)
      }

      "must go from YourBankDetailsPage to BankDetailsCheckYourAnswersPage" in {
        navigator.nextPage(YourBankDetailsPage, NormalMode, userAnswers) mustBe routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to DirectDebitSourcePage" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, true)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, NormalMode, checkPage) mustBe routes.DirectDebitSourceController.onPageLoad(NormalMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to BankApprovalPage" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, false)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, NormalMode, checkPage) mustBe routes.BankApprovalController.onPageLoad()
      }

      "must throw error from BankDetailsCheckYourAnswersPage if no option selected" in {
        navigator.nextPage(BankDetailsCheckYourAnswerPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from DirectDebitSourcePage to PaymentReferencePage" in {
        navigator.nextPage(DirectDebitSourcePage, NormalMode, userAnswers) mustBe routes.PaymentReferenceController.onPageLoad(NormalMode)
      }

      "must go from PaymentReferencePage to YearEndAndMonthController" in {
        navigator.nextPage(PaymentReferencePage, NormalMode, UserAnswers("id")) mustBe routes.YearEndAndMonthController.onPageLoad(NormalMode)
      }

    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "must go from YourBankDetailsPage to BankDetailsCheckYourAnswersPage" in {
        navigator.nextPage(YourBankDetailsPage, CheckMode, userAnswers) mustBe routes.BankDetailsCheckYourAnswerController.onPageLoad(CheckMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to DirectDebitSourcePage" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, true)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, CheckMode, checkPage) mustBe routes.DirectDebitSourceController.onPageLoad(NormalMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to BankApprovalPage" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, false)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, CheckMode, checkPage) mustBe routes.BankApprovalController.onPageLoad()
      }

    }
  }
}
