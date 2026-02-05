/*
 * Copyright 2026 HM Revenue & Customs
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

import controllers.actions.*
import models.UserAnswers
import models.responses.PaymentPlanDetails
import pages.{IsSuspensionActivePage, ManagePaymentPlanTypePage, SuspensionDetailsCheckYourAnswerPage, SuspensionPeriodRangeDatePage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, PaymentReferenceSummary, SuspensionPeriodRangeDateSummary}
import views.html.PaymentPlanSuspendedView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PaymentPlanSuspendedController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentPlanSuspendedView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val userAnswers = request.userAnswers

    if (nddsService.suspendPaymentPlanGuard(userAnswers)) {

      val alreadyConfirmed =
        userAnswers.get(SuspensionDetailsCheckYourAnswerPage).contains(true)

      if (userAnswers.get(IsSuspensionActivePage).getOrElse(false) && !alreadyConfirmed) {
        logger.error("Cannot do suspension on already suspended budget plan")
        Redirect(routes.AlreadySuspendedErrorController.onPageLoad())

      } else {

        val maybeResult = for {
          planDetails           <- userAnswers.get(PaymentPlanDetailsQuery)
          suspensionPeriodRange <- userAnswers.get(SuspensionPeriodRangeDatePage)
        } yield {

          val messages = messagesApi.preferred(request)
          val dateFormatter = DateTimeFormatter.ofPattern(
            Constants.longDateTimeFormatPattern,
            messages.lang.locale
          )

          val formattedStartDate =
            suspensionPeriodRange.startDate.format(dateFormatter)

          val formattedEndDate =
            suspensionPeriodRange.endDate.format(dateFormatter)
          val paymentReference = planDetails.paymentPlanDetails.paymentReference

          val suspensionIsActiveMode = isSuspendPeriodActive(planDetails.paymentPlanDetails)

          val rows = buildRows(paymentReference, userAnswers, planDetails.paymentPlanDetails)
          Ok(view(formattedStartDate, formattedEndDate, routes.PaymentPlanDetailsController.onPageLoad(), rows, suspensionIsActiveMode))
        }

        maybeResult match {
          case Some(result) => result
          case _            => Redirect(routes.SystemErrorController.onPageLoad())
        }
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: $planType")
      Redirect(routes.SystemErrorController.onPageLoad())
    }

  }

  private def isSuspendPeriodActive(planDetail: PaymentPlanDetails): Boolean = {
    (for {
      suspensionEndDate <- planDetail.suspensionEndDate
    } yield !LocalDate.now().isAfter(suspensionEndDate)).getOrElse(false)
  }

  private def buildRows(paymentPlanReference: String, userAnswers: UserAnswers, paymentPlanDetails: PaymentPlanDetails)(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    val isSuspensionPeriodActive: Boolean = userAnswers.get(PaymentPlanDetailsQuery) match {
      case Some(query) => isSuspendPeriodActive(query.paymentPlanDetails)
      case _           => false
    }
    Seq(
      Some(PaymentReferenceSummary.row(paymentPlanReference)),
      Some(AmendPaymentAmountSummary.row(paymentPlanDetails.planType, paymentPlanDetails.scheduledPaymentAmount)),
      SuspensionPeriodRangeDateSummary.row(userAnswers, false, isSuspensionPeriodActive)
    ).flatten

}
