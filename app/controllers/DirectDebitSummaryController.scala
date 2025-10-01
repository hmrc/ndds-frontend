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
import models.UserAnswers
import pages.DirectDebitSummaryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.NationalDirectDebitService
import queries.{DirectDebitReferenceQuery, PaymentPlansCountQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DirectDebitSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DirectDebitSummaryController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: DirectDebitSummaryView,
                                                       nddService: NationalDirectDebitService,
                                                       sessionRepository: SessionRepository,
                                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    userAnswers.get(DirectDebitReferenceQuery) match {
      case Some(paymentRefNumber) =>
        cleansePaymentReference(userAnswers).flatMap { _ =>
          nddService.retrieveDirectDebitPaymentPlans(paymentRefNumber).flatMap { ddPaymentPlans =>
            for {
              updatedAnswers <- Future.fromTry(userAnswers.set(PaymentPlansCountQuery, ddPaymentPlans.paymentPlanCount))
              updatedAnswers <- Future.fromTry(userAnswers.set(DirectDebitReferenceQuery,paymentRefNumber))
              
              cachedAnswers  <- Future.fromTry(updatedAnswers.set(DirectDebitSummaryPage, ddPaymentPlans.paymentPlanCount))
              _              <- sessionRepository.set(cachedAnswers)
            } yield {
              Ok(
                view(
                  paymentRefNumber,
                  ddPaymentPlans
                )
              )
            }
          }
        }
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(directDebitReference: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updatedAnswers <- Future.fromTry(userAnswers.set(DirectDebitReferenceQuery, directDebitReference))
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.DirectDebitSummaryController.onPageLoad())
  }

  private def cleansePaymentReference(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      updatedUserAnswers <- Future.fromTry(userAnswers.remove(PaymentReferenceQuery))
      _ <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers
}
