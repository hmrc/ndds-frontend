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
import forms.DirectDebitSourceFormProvider
import models.{DirectDebitSource, Mode, UserAnswers}
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.DirectDebitReferenceQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DirectDebitSourceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DirectDebitSourceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DirectDebitSourceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DirectDebitSourceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[DirectDebitSource] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val answers = request.userAnswers
    val preparedForm = answers.get(DirectDebitSourcePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val backlinkCall = answers.get(DirectDebitReferenceQuery) match {
      case Some(directDebitReference) => routes.DirectDebitSummaryController.onPageLoad()
      case _                          => routes.ConfirmAuthorityController.onPageLoad(mode)
    }

    Ok(view(preparedForm, mode, backlinkCall))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val backlinkCall = request.userAnswers.get(DirectDebitReferenceQuery) match {
            case Some(directDebitReference) => routes.DirectDebitSummaryController.onPageLoad()
            case _                          => routes.ConfirmAuthorityController.onPageLoad(mode)
          }

          Future.successful(BadRequest(view(formWithErrors, mode, backlinkCall)))
        },
        value => {
          val originalAnswers = request.userAnswers

          Future
            .fromTry(setDirectDebitSource(originalAnswers, value))
            .flatMap { updatedAnswers =>
              sessionRepository.set(updatedAnswers).map { _ =>
                Redirect(navigator.nextPage(DirectDebitSourcePage, mode, updatedAnswers))
              }
            }
            .recoverWith { case ex =>
              logger.error("Failed to update UserAnswers in setDirectDebitSource", ex)
              Future.successful(InternalServerError("Unable to update data"))
            }
        }
      )
  }

  private def setDirectDebitSource(userAnswers: UserAnswers, newValue: DirectDebitSource): Try[UserAnswers] = {
    val oldValue = userAnswers.get(DirectDebitSourcePage)
    if (oldValue.contains(newValue)) {
      userAnswers.set(DirectDebitSourcePage, newValue)
    } else {
      userAnswers
        .remove(PaymentPlanTypePage)
        .flatMap(_.remove(PaymentReferencePage))
        .flatMap(_.remove(PaymentsFrequencyPage))
        .flatMap(_.remove(TotalAmountDuePage))
        .flatMap(_.remove(PlanStartDatePage))
        .flatMap(_.remove(AddPaymentPlanEndDatePage))
        .flatMap(_.remove(PlanEndDatePage))
        .flatMap(_.remove(PaymentPlanTypePage))
        .flatMap(_.remove(PaymentDatePage))
        .flatMap(_.remove(YearEndAndMonthPage))
        .flatMap(_.remove(RegularPaymentAmountPage))
        .flatMap(_.remove(PaymentAmountPage))
        .flatMap(cleanedAnswers => cleanedAnswers.set(DirectDebitSourcePage, newValue))
    }
  }
}
