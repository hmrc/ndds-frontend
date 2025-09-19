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
import pages.AmendPaymentAmountPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentReferenceQuery
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentPlanDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanDetailsController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PaymentPlanDetailsView,
                                       nddService: NationalDirectDebitService,
                                       sessionRepository: SessionRepository,
                                     ) (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(PaymentReferenceQuery) match {
        case Some(reference) =>
          nddService.getPaymentPlanDetails(reference) flatMap { paymentPlanDetails =>
            for {
//              updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPaymentAmountPage, paymentPlanDetails.planType))
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AmendPaymentAmountPage, paymentPlanDetails.scheduledPaymentAmount))
//              updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanStartDatePage, paymentPlanDetails.scheduledPaymentStartDate))
//              updatedAnswers <- Future.fromTry(updatedAnswers.set(AmendPlanEndDatePage, paymentPlanDetails.scheduledPaymentEndDate))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Ok(view(reference, paymentPlanDetails))
          }
        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onRedirect(paymentReference: String): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentReferenceQuery, paymentReference))
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.PaymentPlanDetailsController.onPageLoad())
  }
}
