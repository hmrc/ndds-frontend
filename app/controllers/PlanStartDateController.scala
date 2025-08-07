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
import forms.PlanStartDateFormProvider
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.{BudgetPaymentPlan, TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers}
import navigation.Navigator
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RDSDatacacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.PlanStartDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlanStartDateController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: PlanStartDateFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: PlanStartDateView,
                                         rdsDatacacheService: RDSDatacacheService
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      val form = formProvider()
      val answers = request.userAnswers
      val preparedForm = answers.get(PlanStartDatePage) match {
        case None => form
        case Some(value) => form.fill(value.enteredDate)
      }

      rdsDatacacheService.getEarliestPlanStartDate(request.userAnswers) map { earliestPlanStartDate =>
        Ok(view(
          preparedForm,
          mode,
          DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
          DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
          answers.get(DirectDebitSourcePage).getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers")),
          backLinkRedirect(mode, answers)
        ))
      } recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  private def backLinkRedirect(mode: Mode, answers: UserAnswers) = {
    val optPaymentType = answers.get(PaymentPlanTypePage)
    val optSourceType = answers.get(DirectDebitSourcePage)
    (optSourceType, optPaymentType) match {
      case (Some(MGD), Some(VariablePaymentPlan)) =>
        routes.PaymentReferenceController.onPageLoad(mode)
      case (Some(SA), Some(BudgetPaymentPlan)) =>
        routes.RegularPaymentAmountController.onPageLoad(mode)
      case (Some(TC), Some(TaxCreditRepaymentPlan)) =>
        routes.TotalAmountDueController.onPageLoad(mode)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          rdsDatacacheService.getEarliestPlanStartDate(request.userAnswers) map { earliestPlanStartDate =>
            BadRequest(view(
              formWithErrors,
              mode,
              DateTimeFormats.formattedDateTimeShort(earliestPlanStartDate.date),
              DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
              request.userAnswers.get(DirectDebitSourcePage).getOrElse(throw new Exception("DirectDebitSourcePage details missing from user answers")),
              backLinkRedirect(mode, request.userAnswers)
            ))
          },
        value =>
          for {
            earliestPlanStartDate <- rdsDatacacheService.getEarliestPlanStartDate(request.userAnswers)
            updatedAnswers <- Future.fromTry(request.userAnswers
              .set(PlanStartDatePage, PlanStartDateDetails(value, earliestPlanStartDate.date)))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(PlanStartDatePage, mode, updatedAnswers))
      ) recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }
}
