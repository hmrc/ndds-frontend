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
import controllers.testonly.routes as testOnlyRoutes
import pages.*
import models.*
import models.DirectDebitSource.*
import models.PaymentPlanType.*

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: (Page, Boolean) => UserAnswers => Call = {
    case (PaymentDatePage, _)                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case (PaymentReferencePage, _)                 => userAnswers => checkPaymentReferenceLogic(userAnswers)
    case (PaymentAmountPage, _)                    => _ => routes.PaymentDateController.onPageLoad(NormalMode)
    case (PersonalOrBusinessAccountPage, _)        => _ => routes.YourBankDetailsController.onPageLoad(NormalMode)
    case (YourBankDetailsPage, _)                  => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode)
    case (BankDetailsCheckYourAnswerPage, _)       => _ => routes.ConfirmAuthorityController.onPageLoad(NormalMode)
    case (ConfirmAuthorityPage, _)                 => nextAfterConfirmAuthority(NormalMode)
    case (DirectDebitSourcePage, _)                => checkDirectDebitSource
    case (PaymentPlanTypePage, _)                  => _ => routes.PaymentReferenceController.onPageLoad(NormalMode)
    case (PaymentsFrequencyPage, _)                => _ => routes.RegularPaymentAmountController.onPageLoad(NormalMode)
    case (RegularPaymentAmountPage, _)             => _ => routes.PlanStartDateController.onPageLoad(NormalMode)
    case (TotalAmountDuePage, _)                   => _ => routes.PlanStartDateController.onPageLoad(NormalMode)
    case (PlanStartDatePage, _)                    => userAnswers => navigateFromPlanStartDatePage(NormalMode)(userAnswers)
    case (PlanEndDatePage, _)                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case (YearEndAndMonthPage, _)                  => _ => routes.PaymentAmountController.onPageLoad(NormalMode)
    case (AmendPaymentAmountPage, _)               => userAnswers => checkPaymentPlanLogic(userAnswers, NormalMode)
    case (AmendPlanStartDatePage, _)               => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode)
    case (AddPaymentPlanEndDatePage, _)            => userAnswers => navigateFromAddPaymentPlanEndDatePage(NormalMode)(userAnswers)
    case (AmendPlanEndDatePage, false)             => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode)
    case (AmendPlanEndDatePage, true)              => _ => testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()
    case (SuspensionPeriodRangeDatePage, _)        => _ => routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode)
    case (SuspensionDetailsCheckYourAnswerPage, _) => _ => routes.PaymentPlanSuspendedController.onPageLoad()
    case (CancelPaymentPlanPage, _)                => navigateFromCancelPaymentPlanPage
    case (RemovingThisSuspensionPage, _)           => navigateFromRemovingThisSuspensionPage
    case (ConfirmRemovePlanEndDatePage, _)         => navigateFromConfirmRemovePlanEndDatePage
    case (TellAboutThisPaymentPage, _)             => navigateTellAboutThisPaymentPage
    case _                                         => _ => routes.LandingController.onPageLoad()
  }

  private val checkRouteMap: (Page, Boolean) => UserAnswers => Call = {
    case (YourBankDetailsPage, _)                  => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(CheckMode)
    case (BankDetailsCheckYourAnswerPage, _)       => _ => routes.ConfirmAuthorityController.onPageLoad(CheckMode)
    case (ConfirmAuthorityPage, _)                 => nextAfterConfirmAuthority(CheckMode)
    case (DirectDebitSourcePage, _)                => checkDirectDebitSource
    case (PaymentPlanTypePage, _)                  => _ => routes.PaymentReferenceController.onPageLoad(NormalMode)
    case (PaymentReferencePage, _)                 => _ => routes.CheckYourAnswersController.onPageLoad()
    case (PaymentAmountPage, _)                    => _ => routes.CheckYourAnswersController.onPageLoad()
    case (PaymentDatePage, _)                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case (PlanStartDatePage, _)                    => userAnswers => navigateFromPlanStartDatePage(CheckMode)(userAnswers)
    case (PlanEndDatePage, _)                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case (TotalAmountDuePage, _)                   => _ => routes.CheckYourAnswersController.onPageLoad()
    case (PaymentsFrequencyPage, _)                => _ => routes.CheckYourAnswersController.onPageLoad()
    case (RegularPaymentAmountPage, _)             => _ => routes.CheckYourAnswersController.onPageLoad()
    case (YearEndAndMonthPage, _)                  => _ => routes.CheckYourAnswersController.onPageLoad()
    case (SuspensionDetailsCheckYourAnswerPage, _) => _ => routes.PaymentPlanSuspendedController.onPageLoad()
    case (AmendPaymentAmountPage, _)               => userAnswers => checkPaymentPlanLogic(userAnswers, CheckMode)
    case (AmendPlanStartDatePage, _)               => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad(CheckMode)
    case (AddPaymentPlanEndDatePage, _)            => userAnswers => navigateFromAddPaymentPlanEndDatePage(CheckMode)(userAnswers)
    case (AmendPlanEndDatePage, false)             => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad(CheckMode)
    case (AmendPlanEndDatePage, true)              => _ => testOnlyRoutes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()
    case (SuspensionPeriodRangeDatePage, _)        => _ => routes.CheckYourSuspensionDetailsController.onPageLoad(CheckMode)
    case (RemovingThisSuspensionPage, _)           => navigateFromRemovingThisSuspensionPage
    case (ConfirmRemovePlanEndDatePage, _)         => navigateFromConfirmRemovePlanEndDatePage
    case _                                         => _ => routes.LandingController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, testOnly: Boolean = false): Call = mode match {
    case NormalMode =>
      normalRoutes(page, testOnly)(userAnswers)
    case CheckMode =>
      checkRouteMap(page, testOnly)(userAnswers)
  }

  private def checkPaymentReferenceLogic(userAnswers: UserAnswers): Call = {
    val sourceType = userAnswers.get(DirectDebitSourcePage)
    val optPaymentType = userAnswers.get(PaymentPlanTypePage)
    sourceType match {
      case Some(OL) | Some(NIC) | Some(CT) | Some(SDLT) | Some(VAT) => routes.PaymentAmountController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.MGD) if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan) =>
        routes.PaymentAmountController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.SA) if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan) =>
        routes.PaymentAmountController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.TC) if optPaymentType.contains(PaymentPlanType.SinglePaymentPlan) =>
        routes.PaymentAmountController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.MGD) if optPaymentType.contains(PaymentPlanType.VariablePaymentPlan) =>
        routes.PlanStartDateController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.PAYE) => routes.TellAboutThisPaymentController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.SA) if optPaymentType.contains(PaymentPlanType.BudgetPaymentPlan) =>
        routes.PaymentsFrequencyController.onPageLoad(NormalMode)
      case Some(DirectDebitSource.TC) if optPaymentType.contains(PaymentPlanType.TaxCreditRepaymentPlan) =>
        routes.TotalAmountDueController.onPageLoad(NormalMode)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def nextAfterConfirmAuthority(mode: Mode): UserAnswers => Call = ua =>
    ua.get(ConfirmAuthorityPage) match {
      case Some(ConfirmAuthority.Yes) => routes.DirectDebitSourceController.onPageLoad(mode)
      case Some(ConfirmAuthority.No)  => routes.BankApprovalController.onPageLoad()
      case None                       => routes.JourneyRecoveryController.onPageLoad()
    }

  private def checkDirectDebitSource(userAnswers: UserAnswers): Call =
    val answer: Option[DirectDebitSource] = userAnswers.get(DirectDebitSourcePage)
    answer match {
      case Some(MGD) | Some(SA) | Some(TC) => routes.PaymentPlanTypeController.onPageLoad(NormalMode)
      case _                               => routes.PaymentReferenceController.onPageLoad(NormalMode)
    }

  private def checkPaymentPlanLogic(userAnswers: UserAnswers, mode: Mode): Call = {
    val paymentPlanType = userAnswers.get(ManagePaymentPlanTypePage)
    paymentPlanType match {
      case Some(PaymentPlanType.BudgetPaymentPlan.toString) => routes.AmendPlanEndDateController.onPageLoad(mode)
      case Some(PaymentPlanType.SinglePaymentPlan.toString) => routes.AmendPlanStartDateController.onPageLoad(mode)
      case _                                                => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigateFromAddPaymentPlanEndDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(AddPaymentPlanEndDatePage) match {
      case Some(true)  => routes.PlanEndDateController.onPageLoad(mode)
      case Some(false) => routes.CheckYourAnswersController.onPageLoad()
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromPlanStartDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(DirectDebitSourcePage) match {
      case Some(DirectDebitSource.SA) => routes.AddPaymentPlanEndDateController.onPageLoad(mode)
      case Some(_)                    => routes.CheckYourAnswersController.onPageLoad()
      case None                       => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromCancelPaymentPlanPage(answers: UserAnswers): Call =
    answers
      .get(CancelPaymentPlanPage)
      .map {
        case true  => routes.PaymentPlanCancelledController.onPageLoad()
        case false => routes.PaymentPlanDetailsController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromConfirmRemovePlanEndDatePage(answers: UserAnswers): Call =
    answers
      .get(ConfirmRemovePlanEndDatePage)
      .map {
        case true  => controllers.testonly.routes.TestOnlyAmendPaymentPlanConfirmationController.onPageLoad()
        case false => controllers.testonly.routes.TestOnlyAmendingPaymentPlanController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromRemovingThisSuspensionPage(answers: UserAnswers): Call =
    answers
      .get(RemovingThisSuspensionPage)
      .map {
        case true  => routes.RemoveSuspensionConfirmationController.onPageLoad()
        case false => routes.PaymentPlanDetailsController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def navigateTellAboutThisPaymentPage(answers: UserAnswers): Call =
    answers
      .get(TellAboutThisPaymentPage)
      .map {
        case true  => routes.YearEndAndMonthController.onPageLoad(NormalMode) // change it when page ready
        case false => routes.PaymentAmountController.onPageLoad(NormalMode)
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

}
