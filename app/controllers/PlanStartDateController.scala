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
import forms.PlanStartDateFormProvider
import models.DirectDebitSource.{MGD, SA, TC}
import models.PaymentPlanType.{BudgetPaymentPlan, TaxCreditRepaymentPlan, VariablePaymentPlan}
import models.{DirectDebitSource, Mode, PlanStartDateDetails, UserAnswers}
import navigation.Navigator
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PlanStartDatePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import views.html.PlanStartDateView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlanStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PlanStartDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PlanStartDateView,
  nddService: NationalDirectDebitService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    {
      val answers = request.userAnswers

      answers.get(DirectDebitSourcePage) match {
        case Some(value)
            if Set(DirectDebitSource.MGD.toString, DirectDebitSource.SA.toString, DirectDebitSource.TC.toString).contains(value.toString) =>
          nddService.getFutureWorkingDays(answers, request.userId) map {
            case Some(earliestPlanStartDate) =>
              val earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
              val form = formProvider(answers, earliestDate)
              val preparedForm = answers.get(PlanStartDatePage) match {
                case None        => form
                case Some(value) => form.fill(value.enteredDate)
              }
              Ok(
                view(
                  preparedForm,
                  mode,
                  DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
                  value,
                  backLinkRedirect(mode, answers)
                )
              )
            case None => Redirect(routes.SystemErrorController.onPageLoad())
          } recover { case e =>
            logger.warn(s"Unexpected error: $e")
            Redirect(routes.SystemErrorController.onPageLoad())
          }
        case _ =>
          logger.warn(s"DirectDebitSource is missing or not MGD or SA or TC")
          Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
      }
    }
  }

  private def backLinkRedirect(mode: Mode, answers: UserAnswers) = {
    val optPaymentType = answers.get(PaymentPlanTypePage)
    val optSourceType = answers.get(DirectDebitSourcePage)
    (optSourceType, optPaymentType) match {
      case (Some(MGD), Some(VariablePaymentPlan))   => routes.PaymentReferenceController.onPageLoad(mode)
      case (Some(SA), Some(BudgetPaymentPlan))      => routes.RegularPaymentAmountController.onPageLoad(mode)
      case (Some(TC), Some(TaxCreditRepaymentPlan)) => routes.TotalAmountDueController.onPageLoad(mode)
      case _                                        => routes.SystemErrorController.onPageLoad()
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    nddService
      .getFutureWorkingDays(request.userAnswers, request.userId)
      .flatMap {
        case None => Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))

        case Some(earliestPlanStartDate) => {
          val earliestDate = LocalDate.parse(earliestPlanStartDate.date, DateTimeFormatter.ISO_LOCAL_DATE)
          val form = formProvider(request.userAnswers, earliestDate)

          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                request.userAnswers.get(DirectDebitSourcePage) match {
                  case Some(directDebitSource) =>
                    Future.successful(
                      BadRequest(
                        view(
                          formWithErrors,
                          mode,
                          DateTimeFormats.formattedDateTimeNumeric(earliestPlanStartDate.date),
                          directDebitSource,
                          backLinkRedirect(mode, request.userAnswers)
                        )
                      )
                    )

                  case None =>
                    logger.warn("DirectDebitSourcePage missing from user answers")
                    Future.successful(Redirect(routes.SystemErrorController.onPageLoad()))
                }
              },
              value =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(PlanStartDatePage, PlanStartDateDetails(value, earliestPlanStartDate.date)))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(PlanStartDatePage, mode, updatedAnswers))
            )
        }
      }
      .recover { case e =>
        logger.warn(s"Unexpected error: $e")
        Redirect(routes.SystemErrorController.onPageLoad())
      }
  }

}
