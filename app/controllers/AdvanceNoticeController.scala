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
import models.requests.ChrisSubmissionRequest
import models.responses.{DirectDebitDetails, PaymentPlanDetails}
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetails, YourBankDetailsWithAuddisStatus}
import pages.*
import play.api.Logging
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{AdvanceNoticeResponseQuery, DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import viewmodels.checkAnswers.*
import views.html.AdvanceNoticeView

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AdvanceNoticeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  nddService: NationalDirectDebitService,
  view: AdvanceNoticeView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val userAnswers = request.userAnswers
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
    val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

    userAnswers.get(PaymentPlanDetailsQuery) match {
      case Some(response) =>
        val planDetail = response.paymentPlanDetails
        val directDebitDetails = response.directDebitDetails
        val totalAmount: String = userAnswers.get(AdvanceNoticeResponseQuery).flatMap(_.totalAmount).map(currencyFormat.format).getOrElse("£0.00")
        val dueDate: String = userAnswers.get(AdvanceNoticeResponseQuery).flatMap(_.dueDate).map(_.format(dateFormat)).getOrElse("")

        for {
          directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
          paymentPlanReference <- Future.fromTry(Try(userAnswers.get(PaymentPlanReferenceQuery).get))
          planType             <- Future.fromTry(Try(userAnswers.get(ManagePaymentPlanTypePage).get))
        } yield {
          Ok(
            view(
              appConfig.hmrcHelplineUrl,
              totalAmount,
              dueDate,
              directDebitReference,
              directDebitDetails.bankAccountName.getOrElse(""),
              directDebitDetails.bankSortCode.getOrElse(""),
              directDebitDetails.bankAccountNumber.getOrElse(""),
              paymentPlanReference,
              routes.PaymentPlanDetailsController.onPageLoad()
            )
          )
        }
    }
  }
}
