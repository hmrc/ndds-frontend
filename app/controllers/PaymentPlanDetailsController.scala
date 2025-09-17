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

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import models.UserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import models.responses.{PaymentPlanDetails, PaymentPlanDetailsResponse}
import viewmodels.{PaymentPlanViewModel, PaymentPlansDetailsViewModel}
import views.html.PaymentPlanDetailsView

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanDetailsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: PaymentPlanDetailsView,
                                              appConfig: FrontendAppConfig,
                                              nddService: NationalDirectDebitService
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {


  def onPageLoad(paymentReference: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val ua: UserAnswers = request.userAnswers
      val call: Call = routes.YourDirectDebitInstructionsController.onPageLoad()
      
      nddService.getPaymentPlanDetails(paymentReference).flatMap { (resp: PaymentPlanDetailsResponse) =>
        Future.traverse(resp.paymentPlanDetails)(plan => toViewModel(plan)).map { planViewModels =>
          Ok(view(
            viewModel = PaymentPlansDetailsViewModel(paymentReference, planViewModels),
            paymentReference = paymentReference,
            call = call,
            userAnswers = ua
          ))
        }
      }
    }

  private def toViewModel(
                           plan: PaymentPlanDetails
                         )(implicit request: Request[_], ua: UserAnswers): Future[PaymentPlanViewModel] =
    for {
      twoWorkingDaysPrior <- twoDaysPriorFlag(plan)
      endWithinThreeWorkingDays <- planEndWithinThreeWorkingDaysFlag(plan)
      (showAmend, showCancel, showSuspend) =
        computeActions(plan, ua, twoWorkingDaysPrior, endWithinThreeWorkingDays)
    } yield PaymentPlanViewModel(
      plan = plan,
      twoDaysPrior = twoWorkingDaysPrior,
      endWithinThreeDays = endWithinThreeWorkingDays,
      showAmend = showAmend,
      showCancel = showCancel,
      showSuspend = showSuspend
    )

  private def twoDaysPriorFlag(
                                plan: PaymentPlanDetails
                              )(implicit request: Request[_], ua: UserAnswers): Future[Boolean] =
    if (nddService.isSinglePaymentPlan(ua)) {
      plan.initialPaymentStartDate
        .map { dt =>
          nddService.paymentDateWithinTwoWorkingDaysFlag(dt, ua).flatMap { v =>
            nddService.saveTwoDaysPriorFlag(v).map(_ => v)
          }
        }
        .getOrElse(Future.successful(false))
    } else {
      Future.successful(false)
    }

  private def planEndWithinThreeWorkingDaysFlag(
                                                 plan: PaymentPlanDetails
                                               )(implicit request: Request[_], ua: UserAnswers): Future[Boolean] =
    if (nddService.isBudgetPaymentPlan(ua)) {
      val endDateOpt: Option[java.time.LocalDateTime] = plan.scheduledPaymentEndDate
      nddService.planEndWithinThreeWorkingDaysFlag(endDateOpt, ua).flatMap { v =>
        nddService.saveEndWithinThreeDaysFlag(v).map(_ => v)
      }
    } else {
      Future.successful(false)
    }

  private def computeActions(
                              plan: PaymentPlanDetails,
                              ua: UserAnswers,
                              twoDaysPrior: Boolean,
                              endWithinThreeDays: Boolean
                            ): (Boolean, Boolean, Boolean) = {
    val blockedByWindow = twoDaysPrior || endWithinThreeDays

    val showAmend =
      nddService.canAmendPaymentPlan(ua) &&
        !blockedByWindow &&
        plan.paymentPlanEditable

    val showCancel =
      nddService.canCancelPaymentPlan(ua) &&
        !blockedByWindow &&
        plan.paymentPlanEditable

    val showSuspend =
      nddService.isBudgetPaymentPlan(ua) &&
        !endWithinThreeDays &&
        plan.paymentPlanEditable

    (showAmend, showCancel, showSuspend)
  }
}
