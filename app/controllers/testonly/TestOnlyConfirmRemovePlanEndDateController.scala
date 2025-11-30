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

package controllers.testonly

import controllers.actions.*
import controllers.routes
import forms.ConfirmRemovePlanEndDateFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.ConfirmRemovePlanEndDatePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats.formattedDateTimeShort
import pages.*
import views.html.testonly.TestOnlyConfirmRemovePlanEndDateView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyConfirmRemovePlanEndDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ConfirmRemovePlanEndDateFormProvider,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyConfirmRemovePlanEndDateView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val userAnswers = request.userAnswers

      if (!nddsService.amendPaymentPlanGuard(userAnswers)) {
        val planType = userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else {

        val maybeResult = for {
          planEndDateValue <- userAnswers.get(AmendPlanEndDatePage)
          planDetails      <- userAnswers.get(PaymentPlanDetailsQuery)
        } yield {
          val planEndDate = formattedDateTimeShort(planEndDateValue.toString)

          val preparedForm =
            userAnswers
              .get(ConfirmRemovePlanEndDatePage)
              .map(form.fill)
              .getOrElse(form)

          Ok(
            view(
              preparedForm,
              mode,
              planDetails.paymentPlanDetails.paymentReference,
              planEndDate,
              controllers.testonly.routes.TestOnlyAmendingPaymentPlanController.onPageLoad()
            )
          )
        }

        maybeResult match {
          case Some(result) => Future.successful(result)
          case None =>
            logger.warn(
              "Missing required values in user answers for ConfirmRemovePlanEndDatePage"
            )
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val ua = request.userAnswers

      val maybeRefs = for {
        planDetails <- ua.get(PaymentPlanDetailsQuery)
        endDate     <- ua.get(AmendPlanEndDatePage)
      } yield {
        val planReference = planDetails.paymentPlanDetails.paymentReference
        val planEndDateStr = formattedDateTimeShort(endDate.toString)
        (planReference, planEndDateStr)
      }

      maybeRefs match {
        case Some((planReference, planEndDateStr)) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      mode,
                      planReference,
                      planEndDateStr,
                      controllers.testonly.routes.TestOnlyAmendingPaymentPlanController.onPageLoad()
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(ua.set(ConfirmRemovePlanEndDatePage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(ConfirmRemovePlanEndDatePage, mode, updatedAnswers))
            )

        case None =>
          logger.warn("Missing PaymentPlanDetails or AmendPlanEndDate in UserAnswers for ConfirmRemovePlanEndDate submission")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

}
