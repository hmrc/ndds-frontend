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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.MaskAndFormatUtils.formatAmount
import views.html.SuspensionPeriodRangeDateView
import play.api.i18n.Lang.logger

import scala.concurrent.{ExecutionContext, Future}

class SuspensionPeriodRangeDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  formProvider: SuspensionPeriodRangeDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SuspensionPeriodRangeDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      request.userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(response) =>
          val planDetail = response.paymentPlanDetails

          // ✅ Only allow suspension for BudgetPaymentPlan
          if (!nddsService.suspendPaymentPlanGuard(request.userAnswers)) {
            logger.error(
              s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${request.userAnswers.get(ManagePaymentPlanTypePage)}"
            )
            Redirect(routes.JourneyRecoveryController.onPageLoad())
          } else {
            val form = formProvider()
            val preparedForm = request.userAnswers.get(SuspensionPeriodRangeDatePage) match {
              case Some(value) => form.fill(value)
              case None        => form
            }

            val (planReference, paymentAmount) = extractPlanData
            Ok(view(preparedForm, mode, planReference, paymentAmount))
          }

        case None =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
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
            val form = formProvider()
            form
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, planReference, paymentAmount))),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(SuspensionPeriodRangeDatePage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(SuspensionPeriodRangeDatePage, mode, updatedAnswers))
              )
          }

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def extractPlanData(implicit request: DataRequest[?]): (String, String) = {
    val planReference = request.userAnswers.get(PaymentPlanReferenceQuery).getOrElse("")
    val planDetailsOpt = request.userAnswers.get(PaymentPlanDetailsQuery)
    val paymentAmount =
      formatAmount(planDetailsOpt.flatMap(_.paymentPlanDetails.scheduledPaymentAmount).getOrElse(BigDecimal(0)))

    (planReference, paymentAmount)
  }

}
