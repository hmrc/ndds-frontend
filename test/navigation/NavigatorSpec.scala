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
import models.DirectDebitSource.*

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

      "must go from DirectDebitSourcePage to PaymentPlanTypePage if source is MGD, SA, TC" in {
        val validSources = Seq(MGD, SA, TC)

        validSources.foreach { source =>
          val ua = userAnswers.set(DirectDebitSourcePage, source).success.value
          navigator.nextPage(DirectDebitSourcePage, NormalMode, ua) mustBe routes.PaymentPlanTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from DirectDebitSourcePage to PaymentPlanTypePage if source is CT, NIC, OL, PAYE, SDLT, VAT" in {
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

      "must go from PaymentReferencePage to PaymentAmountController for MGD, SA, TC with SinglePayment" in {
        val sources = Seq(MGD, SA, TC)

        sources.foreach { source =>
          val ua = userAnswers
            .set(DirectDebitSourcePage, source).success.value
            .set(PaymentPlanTypePage, PaymentPlanType.SinglePayment).success.value
          navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
            routes.PaymentAmountController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PlanStartDatePage for MGD with VariablePaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, MGD).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan).success.value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.PlanStartDateController.onPageLoad(NormalMode)
      }

      "must throw error from PaymentReferencePage if DirectDebitSource is missing" in {
        navigator.nextPage(PaymentReferencePage, NormalMode, userAnswers) mustBe
          routes.JourneyRecoveryController.onPageLoad()
      }

      "go from PaymentAmountPage to PaymentDatePage" in {
        val sources = Seq(MGD, SA, TC)
        sources.foreach { source =>
          val ua = userAnswers
            .set(DirectDebitSourcePage, source).success.value
            .set(PaymentPlanTypePage, PaymentPlanType.SinglePayment).success.value
          navigator.nextPage(PaymentAmountPage, NormalMode, ua) mustBe
            routes.PaymentDateController.onPageLoad(NormalMode)
        }
      }

      "must go from PaymentReferencePage to PaymentsFrequencyPage for SA with BudgetPaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, SA).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.PaymentsFrequencyController.onPageLoad(NormalMode)
      }

      "must go from PaymentReferencePage to PaymentsFrequencyPage for TC with TaxCreditRepaymentPLan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, TC).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan).success.value

        navigator.nextPage(PaymentReferencePage, NormalMode, ua) mustBe
          routes.TotalAmountDueController.onPageLoad(NormalMode)
      }

      "must go from a PaymentDatePage to CheckYourAnswersPage" in {
        navigator.nextPage(PaymentDatePage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanEndDatePage to CheckYourAnswersController" in {
        navigator.nextPage(PlanEndDatePage, NormalMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to PlanEndDateController for SA with BudgetPaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, SA).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan).success.value
        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.PlanEndDateController.onPageLoad(NormalMode)
      }

      "must go from PlanStartDatePage to CheckYourAnswersController for MGD with VariablePaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, MGD).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan).success.value
        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to CheckYourAnswersController for TC with TaxCreditRepaymentPlan" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, TC).success.value
          .set(PaymentPlanTypePage, PaymentPlanType.TaxCreditRepaymentPlan).success.value
        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to CheckYourAnswersController for PAYE" in {
        val ua = userAnswers
          .set(DirectDebitSourcePage, PAYE).success.value
        navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
          routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from PlanStartDatePage to JourneyRecoveryController for all other combinations" in {
        val invalidCombinations = Seq(
          (SA, PaymentPlanType.SinglePayment),
          (MGD, PaymentPlanType.SinglePayment),
          (MGD, PaymentPlanType.BudgetPaymentPlan),
          (TC, PaymentPlanType.SinglePayment),
          (TC, PaymentPlanType.VariablePaymentPlan),
          (CT, PaymentPlanType.SinglePayment),
          (CT, PaymentPlanType.VariablePaymentPlan),
          (CT, PaymentPlanType.BudgetPaymentPlan),
          (CT, PaymentPlanType.TaxCreditRepaymentPlan)
        )
        invalidCombinations.foreach { case (source, planType) =>
          val ua = userAnswers
            .set(DirectDebitSourcePage, source).success.value
            .set(PaymentPlanTypePage, planType).success.value
          navigator.nextPage(PlanStartDatePage, NormalMode, ua) mustBe
            routes.JourneyRecoveryController.onPageLoad()
        }
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

      "must go from PlanEndDatePage to CheckYourAnswersController in CheckMode" in {
        navigator.nextPage(PlanEndDatePage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad()
      }

    }
  }
}
