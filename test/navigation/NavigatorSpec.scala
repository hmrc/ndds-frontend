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
import models.*
import models.DirectDebitSource.*
import pages.*

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator
  val userAnswers: UserAnswers = UserAnswers("id")

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.LandingController.onPageLoad()
      }

      "must go from a SuspensionDetailsCheckYourAnswerPage to PaymentPlanSuspended confirmation page " in {
        navigator.nextPage(SuspensionDetailsCheckYourAnswerPage, NormalMode, userAnswers) mustBe routes.PaymentPlanSuspendedController.onPageLoad()
      }

      "must go from a PersonalOrBusinessAccountPage to YourBankDetailsPage" in {
        navigator.nextPage(PersonalOrBusinessAccountPage, NormalMode, userAnswers) mustBe routes.YourBankDetailsController.onPageLoad(NormalMode)
      }

      "must go from a PaymentFrequencyPage to RegularPaymentAmountPage" in {
        navigator.nextPage(PaymentsFrequencyPage, NormalMode, userAnswers) mustBe routes.RegularPaymentAmountController.onPageLoad(NormalMode)
      }

      "must go from YourBankDetailsPage to BankDetailsCheckYourAnswersPage" in {
        navigator.nextPage(YourBankDetailsPage, NormalMode, userAnswers) mustBe routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode)
      }

      "must go from RegularPaymentAmountPage to PlanStartDatePage" in {
        navigator.nextPage(RegularPaymentAmountPage, NormalMode, userAnswers) mustBe routes.PlanStartDateController.onPageLoad(NormalMode)
      }

      "must go from TotalAmountDuePage to PlanStartDatePage" in {
        navigator.nextPage(TotalAmountDuePage, NormalMode, userAnswers) mustBe routes.PlanStartDateController.onPageLoad(NormalMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to ConfirmAuthority.1" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, true)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, NormalMode, checkPage) mustBe routes.ConfirmAuthorityController.onPageLoad(NormalMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to ConfirmAuthority.2" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, false)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, NormalMode, checkPage) mustBe routes.ConfirmAuthorityController.onPageLoad(NormalMode)
      }

      "must throw error from ConfirmAuthorityPage if no option selected" in {
        navigator.nextPage(ConfirmAuthorityPage, NormalMode, userAnswers) mustBe routes.SystemErrorController.onPageLoad()
      }

      "must go from DirectDebitSourcePage to PaymentPlanTypePage if source is MGD, SA, TC" in {
        val validSources = Seq(MGD, SA, TC)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(DirectDebitSourcePage, NormalMode, ua) mustBe routes.PaymentPlanTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from DirectDebitSourcePage to PaymentReferencePage if source is CT, NIC, OL, PAYE, SDLT, VAT" in {
        val validSources = Seq(CT, NIC, OL, PAYE, SDLT, VAT)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(DirectDebitSourcePage, NormalMode, ua) mustBe routes.PaymentReferenceController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PaymentAmountController for CT, NIC, OL, SDLT, VAT" in {
        val validSources = Seq(CT, NIC, OL, SDLT, VAT)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
            routes.PaymentAmountController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PaymentAmountController for MGD, SA, TC with SinglePaymentPlan" in {
        val sources = Seq(MGD, SA, TC)

        sources.foreach { source =>
          val ua = userAnswers
            .set(DirectDebitSourcePage, source)
            .success
            .value
            .set(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)
            .success
            .value
          navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
            routes.PaymentAmountController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PlanStartDatePage for MGD with VariablePaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, MGD)
          .success
          .value
          .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan)
          .success
          .value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.PlanStartDateController.onPageLoad(NormalMode)
      }

      "must throw error from PaymentReferencePage if DirectDebitSource is missing" in {
        navigator.nextPage(PaymentReferencePage, NormalMode, userAnswers) mustBe
          routes.SystemErrorController.onPageLoad()
      }

      "go from PaymentAmountPage to PaymentDatePage" in {
        val sources = Seq(MGD, SA, TC)
        sources.foreach { source =>
          val ua = userAnswers
            .set(DirectDebitSourcePage, source)
            .success
            .value
            .set(PaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan)
            .success
            .value
          navigator.nextPage(PaymentAmountPage, NormalMode, ua) mustBe
            routes.PaymentDateController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PaymentsFrequencyPage for SA with BudgetPaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, SA)
          .success
          .value
          .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan)
          .success
          .value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.PaymentsFrequencyController.onPageLoad(NormalMode)
      }

      "must go from PaymentReferencePage to PaymentsFrequencyPage for TC with TaxCreditRepaymentPLan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, TC)
          .success
          .value
          .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan)
          .success
          .value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.TotalAmountDueController.onPageLoad(NormalMode)
      }

      "must go from a PaymentDatePage to CheckYourAnswersPage" in {
        navigator.nextPage(PaymentDatePage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanEndDatePage to CheckYourAnswersController" in {
        navigator.nextPage(PlanEndDatePage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from YearEndAndMonthPage to PaymentAmountController" in {
        navigator.nextPage(YearEndAndMonthPage, NormalMode, userAnswers) mustBe routes.PaymentAmountController.onPageLoad(NormalMode)
      }

      "must go from PlanStartDatePage to AddPaymentPlanEndDateController when DirectDebitSource is SA" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, SA)
          .success
          .value

        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.AddPaymentPlanEndDateController.onPageLoad(NormalMode)
      }

      "must go from PlanStartDatePage to CheckYourAnswersController when DirectDebitSource is not SA" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, MGD)
          .success
          .value

        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.CheckYourAnswersController.onPageLoad()
      }

      "Amend Journey " - {

        "must go from a AmendPaymentAmountPage to AmendPlanStartDatePage" in {
          val ua = userAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
            .success
            .value

          navigator.nextPage(AmendPaymentAmountPage, NormalMode, ua) mustBe
            routes.AmendPlanStartDateController.onPageLoad(NormalMode)
        }

        "must go from a AmendPaymentAmountPage to AmendPlanEndDatePage" in {
          val ua = userAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
            .success
            .value

          navigator.nextPage(AmendPaymentAmountPage, NormalMode, ua) mustBe
            routes.AmendPlanEndDateController.onPageLoad(NormalMode)
        }

        "must go from a AmendPaymentAmountPage to System Error Page" in {
          navigator.nextPage(AmendPaymentAmountPage, NormalMode, userAnswers) mustBe
            routes.SystemErrorController.onPageLoad()
        }

        "must go from a AmendPlanStartDatePage to AmendPaymentPlanConfirmationController" in {
          navigator.nextPage(AmendPlanStartDatePage, NormalMode, userAnswers) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a AddPaymentPlanEndDatePage to PlanEndDatePage when Yes" in {
          val ua = userAnswers.set(AddPaymentPlanEndDatePage, true).success.value

          navigator.nextPage(AddPaymentPlanEndDatePage, NormalMode, ua) mustBe
            routes.PlanEndDateController.onPageLoad(NormalMode)
        }

        "must go from a AddPaymentPlanEndDatePage to CheckYourAnswersPage when No" in {
          val ua = userAnswers.set(AddPaymentPlanEndDatePage, false).success.value

          navigator.nextPage(AddPaymentPlanEndDatePage, NormalMode, ua) mustBe
            routes.CheckYourAnswersController.onPageLoad()
        }

        "must go from a AmendPlanEndDatePage to CheckYourAnswersPage" in {
          navigator.nextPage(AmendPlanEndDatePage, NormalMode, userAnswers) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a CancelPaymentPlanPage to PaymentPlanDetailsController" in {
          val ua = userAnswers
            .set(CancelPaymentPlanPage, false)
            .success
            .value

          navigator.nextPage(CancelPaymentPlanPage, NormalMode, ua) mustBe
            routes.PaymentPlanDetailsController.onPageLoad()
        }

        "must go from a CancelPaymentPlanPage to PaymentCancellationSuccessController" in {
          val ua = userAnswers
            .set(CancelPaymentPlanPage, true)
            .success
            .value

          navigator.nextPage(CancelPaymentPlanPage, NormalMode, ua) mustBe
            routes.PaymentPlanCancelledController.onPageLoad()
        }

        "must go from a CancelPaymentPlanPage to System Error Page" in {
          val ua = userAnswers

          navigator.nextPage(CancelPaymentPlanPage, NormalMode, ua) mustBe
            routes.SystemErrorController.onPageLoad()
        }

        "must go from a AmendConfirmRemovePlanEndDatePage to AP2 when Yes selected" in {
          val ua = userAnswers
            .set(AmendConfirmRemovePlanEndDatePage, true)
            .success
            .value

          navigator.nextPage(AmendConfirmRemovePlanEndDatePage, NormalMode, ua) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a AmendConfirmRemovePlanEndDatePage to AP2 when No selected" in {
          val ua = userAnswers
            .set(AmendConfirmRemovePlanEndDatePage, false)
            .success
            .value

          navigator.nextPage(AmendConfirmRemovePlanEndDatePage, NormalMode, ua) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a AmendConfirmRemovePlanEndDatePage to System Error Page when no answer" in {
          val ua = userAnswers

          navigator.nextPage(AmendConfirmRemovePlanEndDatePage, NormalMode, ua) mustBe
            routes.SystemErrorController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to PaymentPlanDetailsController when No" in {
          val ua = userAnswers
            .set(RemovingThisSuspensionPage, false)
            .success
            .value

          navigator.nextPage(RemovingThisSuspensionPage, NormalMode, ua) mustBe
            routes.PaymentPlanDetailsController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to RemoveSuspensionConfirmationController when Yes" in {
          val ua = userAnswers
            .set(RemovingThisSuspensionPage, true)
            .success
            .value

          navigator.nextPage(RemovingThisSuspensionPage, NormalMode, ua) mustBe
            routes.RemoveSuspensionConfirmationController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to System Error Page when no answer" in {
          val ua = userAnswers

          navigator.nextPage(RemovingThisSuspensionPage, NormalMode, ua) mustBe
            routes.SystemErrorController.onPageLoad()
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, userAnswers) mustBe routes.LandingController.onPageLoad()
      }

      "must go from YourBankDetailsPage to BankDetailsCheckYourAnswersPage - Checkmode" in {
        navigator.nextPage(YourBankDetailsPage, CheckMode, userAnswers) mustBe routes.BankDetailsCheckYourAnswerController.onPageLoad(CheckMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to ConfirmAuthorityPage.1" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, true)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, CheckMode, checkPage) mustBe routes.ConfirmAuthorityController.onPageLoad(CheckMode)
      }

      "must go from BankDetailsCheckYourAnswersPage to ConfirmAuthorityPage.2" in {
        val checkPage = userAnswers.setOrException(BankDetailsCheckYourAnswerPage, false)
        navigator.nextPage(BankDetailsCheckYourAnswerPage, CheckMode, checkPage) mustBe routes.ConfirmAuthorityController.onPageLoad(CheckMode)
      }

      "must go from DirectDebitSourcePage to PaymentPlanTypePage if source is MGD, SA, TC" in {
        val validSources = Seq(MGD, SA, TC)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(DirectDebitSourcePage, CheckMode, ua) mustBe routes.PaymentPlanTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from DirectDebitSourcePage to PaymentReferencePage if source is CT, NIC, OL, PAYE, SDLT, VAT" in {
        val validSources = Seq(CT, NIC, OL, PAYE, SDLT, VAT)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(DirectDebitSourcePage, CheckMode, ua) mustBe routes.PaymentReferenceController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PaymentReferencePage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PaymentAmountPage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PaymentAmountPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PaymentDatePage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PaymentDatePage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to AddPaymentPlanEndDateController in CheckMode when DirectDebitSource is SA" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, SA)
          .success
          .value

        navigator.nextPage(PlanStartDatePage, CheckMode, ua) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to CheckYourAnswersController in CheckMode when DirectDebitSource is not SA" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, TC)
          .success
          .value

        navigator.nextPage(PlanStartDatePage, CheckMode, ua) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanEndDatePage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PlanEndDatePage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from TotalAmountDuePage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(TotalAmountDuePage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PaymentsFrequencyPage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PaymentsFrequencyPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from RegularPaymentAmountPage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(RegularPaymentAmountPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from YearEndAndMonthPage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(YearEndAndMonthPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "Amend Journey " - {

        "must go from a AmendPaymentAmountPage to AmendPlanStartDatePage" in {
          val ua = userAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
            .success
            .value

          navigator.nextPage(AmendPaymentAmountPage, CheckMode, ua) mustBe
            routes.AmendPlanStartDateController.onPageLoad(CheckMode)
        }

        "must go from a AmendPaymentAmountPage to AmendPlanEndDatePage" in {
          val ua = userAnswers
            .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
            .success
            .value

          navigator.nextPage(AmendPaymentAmountPage, CheckMode, ua) mustBe
            routes.AmendPlanEndDateController.onPageLoad(CheckMode)
        }

        "must go from a AmendPaymentAmountPage to System Error Page" in {
          navigator.nextPage(AmendPaymentAmountPage, CheckMode, userAnswers) mustBe
            routes.SystemErrorController.onPageLoad()
        }

        "must go from a AmendPlanStartDatePage to AmendPaymentPlanConfirmationController" in {
          navigator.nextPage(AmendPlanStartDatePage, CheckMode, userAnswers) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a AddPaymentPlanEndDatePage to PlanEndDatePage when Yes" in {
          val ua = userAnswers.set(AddPaymentPlanEndDatePage, true).success.value

          navigator.nextPage(AddPaymentPlanEndDatePage, CheckMode, ua) mustBe
            routes.PlanEndDateController.onPageLoad(CheckMode)
        }

        "must go from a AddPaymentPlanEndDatePage to CheckYourAnswersPage when No" in {
          val ua = userAnswers.set(AddPaymentPlanEndDatePage, false).success.value

          navigator.nextPage(AddPaymentPlanEndDatePage, CheckMode, ua) mustBe
            routes.CheckYourAnswersController.onPageLoad()
        }

        "must go from a AmendPlanEndDatePage to CheckYourAnswersPage" in {
          navigator.nextPage(AmendPlanEndDatePage, CheckMode, userAnswers) mustBe
            routes.AmendPaymentPlanConfirmationController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to PaymentPlanDetailsController when No in Check mode" in {
          val ua = userAnswers
            .set(RemovingThisSuspensionPage, false)
            .success
            .value

          navigator.nextPage(RemovingThisSuspensionPage, CheckMode, ua) mustBe
            routes.PaymentPlanDetailsController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to RemoveSuspensionConfirmationController when Yes in Check mode" in {
          val ua = userAnswers
            .set(RemovingThisSuspensionPage, true)
            .success
            .value

          navigator.nextPage(RemovingThisSuspensionPage, CheckMode, ua) mustBe
            routes.RemoveSuspensionConfirmationController.onPageLoad()
        }

        "must go from a RemovingThisSuspensionPage to System Error Page when no answer in Check mode" in {
          val ua = userAnswers

          navigator.nextPage(RemovingThisSuspensionPage, CheckMode, ua) mustBe
            routes.SystemErrorController.onPageLoad()
        }

        "must go from TellAboutThisPaymentPage to PaymentAmountPage for PAYE if No Selected in Normal mode" in {
          val ua = userAnswers
            .set(DirectDebitSourcePage, PAYE)
            .success
            .value
            .set(TellAboutThisPaymentPage, false)
            .success
            .value

          navigator.nextPage(TellAboutThisPaymentPage, NormalMode, ua) mustBe
            routes.PaymentAmountController.onPageLoad(NormalMode)
        }

        "must go from TellAboutThisPaymentPage to YearEndAndMonthPage for PAYE if Yes Selected in Normal mode" in {
          val ua = userAnswers
            .set(DirectDebitSourcePage, PAYE)
            .success
            .value
            .set(TellAboutThisPaymentPage, true)
            .success
            .value

          navigator.nextPage(TellAboutThisPaymentPage, NormalMode, ua) mustBe
            routes.YearEndAndMonthController.onPageLoad(NormalMode)
        }

        "must go from TellAboutThisPaymentPage to CheckYourAnswersPage for PAYE if No Selected in Check mode" in {
          val ua = userAnswers
            .set(DirectDebitSourcePage, PAYE)
            .success
            .value
            .set(TellAboutThisPaymentPage, false)
            .success
            .value

          navigator.nextPage(TellAboutThisPaymentPage, CheckMode, ua) mustBe
            routes.CheckYourAnswersController.onPageLoad()
        }

        "must go from TellAboutThisPaymentPage to YearEndAndMonthPage for PAYE if Yes Selected in Check mode" in {
          val ua = userAnswers
            .set(DirectDebitSourcePage, PAYE)
            .success
            .value
            .set(TellAboutThisPaymentPage, true)
            .success
            .value

          navigator.nextPage(TellAboutThisPaymentPage, CheckMode, ua) mustBe
            routes.YearEndAndMonthController.onPageLoad(CheckMode)
        }

      }
    }
  }
}
