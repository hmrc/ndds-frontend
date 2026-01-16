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

import controllers.routes
import models.*
import models.DirectDebitSource.*
import models.PaymentPlanType.*
import pages.*
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case PaymentDatePage                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case PaymentReferencePage                 => userAnswers => checkPaymentReferenceLogic(userAnswers)
    case PaymentAmountPage                    => _ => routes.PaymentDateController.onPageLoad(NormalMode)
    case PersonalOrBusinessAccountPage        => _ => routes.YourBankDetailsController.onPageLoad(NormalMode)
    case YourBankDetailsPage                  => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(NormalMode)
    case BankDetailsCheckYourAnswerPage       => _ => routes.ConfirmAuthorityController.onPageLoad(NormalMode)
    case ConfirmAuthorityPage                 => nextAfterConfirmAuthority(NormalMode)
    case DirectDebitSourcePage                => checkDirectDebitSource
    case PaymentPlanTypePage                  => _ => routes.PaymentReferenceController.onPageLoad(NormalMode)
    case PaymentsFrequencyPage                => _ => routes.RegularPaymentAmountController.onPageLoad(NormalMode)
    case RegularPaymentAmountPage             => _ => routes.PlanStartDateController.onPageLoad(NormalMode)
    case TotalAmountDuePage                   => _ => routes.PlanStartDateController.onPageLoad(NormalMode)
    case PlanStartDatePage                    => userAnswers => navigateFromPlanStartDatePage(NormalMode)(userAnswers)
    case PlanEndDatePage                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case YearEndAndMonthPage                  => _ => routes.PaymentAmountController.onPageLoad(NormalMode)
    case AmendPaymentAmountPage               => userAnswers => checkPaymentPlanLogic(userAnswers, NormalMode)
    case AmendPlanStartDatePage               => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad()
    case AddPaymentPlanEndDatePage            => userAnswers => navigateFromAddPaymentPlanEndDatePage(NormalMode)(userAnswers)
    case AmendPlanEndDatePage                 => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad()
    case SuspensionPeriodRangeDatePage        => _ => routes.CheckYourSuspensionDetailsController.onPageLoad(NormalMode)
    case SuspensionDetailsCheckYourAnswerPage => _ => routes.PaymentPlanSuspendedController.onPageLoad()
    case CancelPaymentPlanPage                => navigateFromCancelPaymentPlanPage
    case RemovingThisSuspensionPage           => navigateFromRemovingThisSuspensionPage
    case AmendConfirmRemovePlanEndDatePage    => userAnswers => navigateFromConfirmRemovePlanEndDatePage(NormalMode)(userAnswers)
    case TellAboutThisPaymentPage             => userAnswers => navigateTellAboutThisPaymentPage(NormalMode)(userAnswers)
    case _                                    => _ => routes.LandingController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case YourBankDetailsPage                  => _ => routes.BankDetailsCheckYourAnswerController.onPageLoad(CheckMode)
    case BankDetailsCheckYourAnswerPage       => _ => routes.ConfirmAuthorityController.onPageLoad(CheckMode)
    case ConfirmAuthorityPage                 => nextAfterConfirmAuthority(CheckMode)
    case DirectDebitSourcePage                => checkDirectDebitSource
    case PaymentPlanTypePage                  => _ => routes.PaymentReferenceController.onPageLoad(NormalMode)
    case PaymentReferencePage                 => _ => routes.CheckYourAnswersController.onPageLoad()
    case PaymentAmountPage                    => _ => routes.CheckYourAnswersController.onPageLoad()
    case PaymentDatePage                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case PlanStartDatePage                    => userAnswers => navigateFromPlanStartDatePage(CheckMode)(userAnswers)
    case PlanEndDatePage                      => _ => routes.CheckYourAnswersController.onPageLoad()
    case TotalAmountDuePage                   => _ => routes.CheckYourAnswersController.onPageLoad()
    case PaymentsFrequencyPage                => _ => routes.CheckYourAnswersController.onPageLoad()
    case RegularPaymentAmountPage             => _ => routes.CheckYourAnswersController.onPageLoad()
    case YearEndAndMonthPage                  => _ => routes.CheckYourAnswersController.onPageLoad()
    case SuspensionDetailsCheckYourAnswerPage => _ => routes.PaymentPlanSuspendedController.onPageLoad()
    case AmendPaymentAmountPage               => userAnswers => checkPaymentPlanLogic(userAnswers, CheckMode)
    case AmendPlanStartDatePage               => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad()
    case AddPaymentPlanEndDatePage            => userAnswers => navigateFromAddPaymentPlanEndDatePage(CheckMode)(userAnswers)
    case AmendPlanEndDatePage                 => _ => routes.AmendPaymentPlanConfirmationController.onPageLoad()
    case SuspensionPeriodRangeDatePage        => _ => routes.CheckYourSuspensionDetailsController.onPageLoad(CheckMode)
    case RemovingThisSuspensionPage           => navigateFromRemovingThisSuspensionPage
    case AmendConfirmRemovePlanEndDatePage    => userAnswers => navigateFromConfirmRemovePlanEndDatePage(CheckMode)(userAnswers)
    case TellAboutThisPaymentPage             => userAnswers => navigateTellAboutThisPaymentPage(CheckMode)(userAnswers)
    case _                                    => _ => routes.LandingController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
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
      case _ => routes.SystemErrorController.onPageLoad()
    }
  }

  private def nextAfterConfirmAuthority(mode: Mode): UserAnswers => Call = ua =>
    ua.get(ConfirmAuthorityPage) match {
      case Some(ConfirmAuthority.Yes) => routes.DirectDebitSourceController.onPageLoad(mode)
      case Some(ConfirmAuthority.No)  => routes.BankApprovalController.onPageLoad()
      case None                       => routes.SystemErrorController.onPageLoad()
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
      case _                                                => routes.SystemErrorController.onPageLoad()
    }
  }

  private def navigateFromAddPaymentPlanEndDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(AddPaymentPlanEndDatePage) match {
      case Some(true)  => routes.PlanEndDateController.onPageLoad(mode)
      case Some(false) => routes.CheckYourAnswersController.onPageLoad()
      case None        => routes.SystemErrorController.onPageLoad()
    }

  private def navigateFromPlanStartDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    userAnswers.get(DirectDebitSourcePage) match {
      case Some(DirectDebitSource.SA) if mode == NormalMode => routes.AddPaymentPlanEndDateController.onPageLoad(mode)
      case Some(_)                                          => routes.CheckYourAnswersController.onPageLoad()
      case None                                             => routes.SystemErrorController.onPageLoad()
    }

  private def navigateFromCancelPaymentPlanPage(answers: UserAnswers): Call =
    answers
      .get(CancelPaymentPlanPage)
      .map {
        case true  => routes.PaymentPlanCancelledController.onPageLoad()
        case false => routes.PaymentPlanDetailsController.onPageLoad()
      }
      .getOrElse(routes.SystemErrorController.onPageLoad())

  private def navigateFromConfirmRemovePlanEndDatePage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(AmendConfirmRemovePlanEndDatePage), mode) match {
      case (Some(true), _)           => routes.AmendPaymentPlanConfirmationController.onPageLoad()
      case (Some(false), NormalMode) => routes.AmendingPaymentPlanController.onPageLoad()
      case (Some(false), CheckMode)  => routes.AmendPaymentPlanConfirmationController.onPageLoad()
      case (None, _)                 => routes.SystemErrorController.onPageLoad()
    }

  private def navigateFromRemovingThisSuspensionPage(answers: UserAnswers): Call =
    answers
      .get(RemovingThisSuspensionPage)
      .map {
        case true  => routes.RemoveSuspensionConfirmationController.onPageLoad()
        case false => routes.PaymentPlanDetailsController.onPageLoad()
      }
      .getOrElse(routes.SystemErrorController.onPageLoad())

  private def navigateTellAboutThisPaymentPage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(TellAboutThisPaymentPage), mode) match {
      case (Some(true), _)           => routes.YearEndAndMonthController.onPageLoad(mode)
      case (Some(false), NormalMode) => routes.PaymentAmountController.onPageLoad(NormalMode)
      case (Some(false), CheckMode)  => routes.CheckYourAnswersController.onPageLoad()
      case (None, _)                 => routes.SystemErrorController.onPageLoad()
    }

}
