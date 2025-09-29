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
import forms.AmendPaymentAmountFormProvider
import models.Mode
import navigation.Navigator
import pages.{AmendPaymentAmountPage, NewAmendPaymentAmountPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendPaymentAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendPaymentAmountController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              sessionRepository: SessionRepository,
                                              navigator: Navigator,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: AmendPaymentAmountFormProvider,
                                              nddsService: NationalDirectDebitService,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: AmendPaymentAmountView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  val form: Form[BigDecimal] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val answers = request.userAnswers
      if (nddsService.amendPaymentPlanGuard(answers)) {
        val preparedForm: Form[BigDecimal] = answers.get(NewAmendPaymentAmountPage)
            .orElse(request.userAnswers.get(AmendPaymentAmountPage))
            .fold(form)(form.fill)

        Ok(view(preparedForm, mode, routes.PaymentPlanDetailsController.onPageLoad()))
      } else {
        val paymentPlanType = answers.get(AmendPaymentPlanTypePage).getOrElse("Missing plan type from user answers")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${paymentPlanType}")
        throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${paymentPlanType}")
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, routes.PaymentPlanDetailsController.onPageLoad()))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(NewAmendPaymentAmountPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(AmendPaymentAmountPage, mode, updatedAnswers))
      )
  }

}
