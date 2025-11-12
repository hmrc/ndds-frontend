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

import controllers.actions.*
import forms.SuspensionPeriodRangeDateFormProvider
import models.requests.DataRequest
import javax.inject.Inject
import models.{Mode, PaymentPlanType}
import navigation.Navigator
import pages.{ManagePaymentPlanTypePage, SuspensionPeriodRangeDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{Constants, MaskAndFormatUtils}
import utils.MaskAndFormatUtils.formatAmount
import views.html.SuspensionPeriodRangeDateView

import scala.concurrent.{ExecutionContext, Future}
import java.time.format.DateTimeFormatter

class SuspensionPeriodRangeDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SuspensionPeriodRangeDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SuspensionPeriodRangeDateView,
  nddsService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(Constants.longDateTimeFormatPattern)

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val userAnswers = request.userAnswers
      if (nddsService.suspendPaymentPlanGuard(userAnswers)) {
        val planDates = userAnswers.get(PaymentPlanDetailsQuery).map(_.paymentPlanDetails)
        val planStart = planDates.flatMap(_.scheduledPaymentStartDate)
        val planEnd = planDates.flatMap(_.scheduledPaymentEndDate)

        val formattedSuspensionStartDate = planDates
          .flatMap(_.suspensionStartDate)
          .map(_.format(dateFormatter))
          .getOrElse("")

        val formattedSuspensionEndDate = planDates
          .flatMap(_.suspensionEndDate)
          .map(_.format(dateFormatter))
          .getOrElse("")

        nddsService.earliestSuspendStartDate().map { earliestStartDate =>
          val form = formProvider(planStart, planEnd, earliestStartDate)

          val preparedForm = userAnswers.get(SuspensionPeriodRangeDatePage) match {
            case Some(value) => form.fill(value)
            case None        => form
          }

          val (planReference, paymentAmount) = extractPlanData

          Ok(
            view(
              preparedForm,
              mode,
              planReference,
              paymentAmount,
              formattedSuspensionStartDate,
              formattedSuspensionEndDate
            )
          )
        }

      } else {
        val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(
          s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: $planType"
        )
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(response) =>
          val planDetail = response.paymentPlanDetails
          if (planDetail.planType != PaymentPlanType.BudgetPaymentPlan.toString) {
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          } else {
            val (planReference, paymentAmount) = extractPlanData
            val planStart = planDetail.scheduledPaymentStartDate
            val planEnd = planDetail.scheduledPaymentEndDate

            val formattedSuspensionStartDate = planDetail.suspensionStartDate
              .map(_.format(dateFormatter))
              .getOrElse("")

            val formattedSuspensionEndDate = planDetail.suspensionEndDate
              .map(_.format(dateFormatter))
              .getOrElse("")

            nddsService.earliestSuspendStartDate().flatMap { earliestStartDate =>
              val form = formProvider(planStart, planEnd, earliestStartDate)

              form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(
                        view(formWithErrors, mode, planReference, paymentAmount, formattedSuspensionStartDate, formattedSuspensionEndDate)
                      )
                    ),
                  value =>
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(SuspensionPeriodRangeDatePage, value))
                      _              <- sessionRepository.set(updatedAnswers)
                    } yield Redirect(navigator.nextPage(SuspensionPeriodRangeDatePage, mode, updatedAnswers))
                )
            }
          }

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def extractPlanData(implicit request: DataRequest[?]): (String, String) = {
    val planReference = request.userAnswers.get(PaymentPlanDetailsQuery).map(_.paymentPlanDetails.paymentReference).getOrElse("")
    val planDetailsOpt = request.userAnswers.get(PaymentPlanDetailsQuery)
    val paymentAmount =
      formatAmount(planDetailsOpt.flatMap(_.paymentPlanDetails.scheduledPaymentAmount).getOrElse(BigDecimal(0)))

    (planReference, paymentAmount)
  }
}
