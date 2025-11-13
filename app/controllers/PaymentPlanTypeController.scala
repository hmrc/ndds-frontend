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
import forms.PaymentPlanTypeFormProvider
import models.{DirectDebitSource, Mode, PaymentPlanType, UserAnswers}
import navigation.Navigator
import pages.{DirectDebitSourcePage, PaymentAmountPage, PaymentDatePage, PaymentPlanTypePage, PaymentReferencePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentPlanTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaymentPlanTypeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PaymentPlanTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentPlanTypeView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[PaymentPlanType] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val answers = request.userAnswers
    val selectedAnswers = answers.get(DirectDebitSourcePage)

    val preparedForm = answers.get(PaymentPlanTypePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }
    Ok(view(preparedForm, mode, selectedAnswers, Some(appConfig.payingHmrcUrl), routes.DirectDebitSourceController.onPageLoad(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val selectedSource = request.userAnswers.get(DirectDebitSourcePage)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(
            BadRequest(view(formWithErrors, mode, selectedSource, None, routes.DirectDebitSourceController.onPageLoad(mode)))
          )
        },
        newValue =>
          Future
            .fromTry(setPaymentPlanType(request.userAnswers, newValue))
            .flatMap { updatedAnswers =>
              sessionRepository.set(updatedAnswers).map { _ =>
                Redirect(navigator.nextPage(PaymentPlanTypePage, mode, updatedAnswers))
              }
            }
            .recoverWith { case ex =>
              logger.error("Failed to update user answers in setPaymentPlanType", ex)
              Future.successful(InternalServerError("Unable to update data"))
            }
      )
  }

  private def setPaymentPlanType(userAnswers: UserAnswers, newValue: PaymentPlanType): Try[UserAnswers] = {
    val oldValue = userAnswers.get(PaymentPlanTypePage)

    if (oldValue.contains(newValue)) {
      userAnswers.set(PaymentPlanTypePage, newValue)
    } else {
      userAnswers
        .remove(PaymentAmountPage)
        .flatMap(_.remove(PaymentReferencePage))
        .flatMap(_.remove(PaymentDatePage))
        .flatMap(_.set(PaymentPlanTypePage, newValue))
    }
  }

}
