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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.DirectDebitReferenceQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AmendPaymentPlanSourceSummary, AmendPaymentPlanTypeSummary, AmendPaymentReferenceSummary, AmendPlanEndDateSummary, AmendPlanStartDateSummary, PaymentAmountSummary, RegularPaymentAmountSummary}
import views.html.ConfirmPaymentPlanAmendmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmPaymentPlanAmendmentController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ConfirmPaymentPlanAmendmentView,
                                       nddService: NationalDirectDebitService
                                     ) (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
      userAnswers.get(DirectDebitReferenceQuery) match {
        case Some(reference) =>
          nddService.retrieveAllDirectDebits(request.userId) map { directDebitDetailsData =>
            val firstMatchingDebit = directDebitDetailsData.directDebitList
              .find(_.ddiRefNumber == reference).map(_.toDirectDebitDetails)
            firstMatchingDebit match {
              case Some(debit) => {

                val rows = Seq(
                  AmendPaymentPlanTypeSummary.row(userAnswers),
                  AmendPaymentPlanSourceSummary.row(userAnswers),
                  AmendPaymentReferenceSummary.row(userAnswers),
                  RegularPaymentAmountSummary.row(userAnswers),
                  PaymentAmountSummary.row(userAnswers),
                  AmendPlanStartDateSummary.row(userAnswers),
                  AmendPlanEndDateSummary.row(userAnswers),
                ).flatten

                //Ok(view(reference, debit, paymentPlanSummaryListRow))
                Ok(view(reference, debit, rows))
              }
              case None => Redirect(routes.JourneyRecoveryController.onPageLoad())
            }
          }

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
