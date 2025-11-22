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

import config.FrontendAppConfig
import controllers.routes
import controllers.actions.*
import models.{PaymentPlanType, UserAnswers}
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery}
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import utils.MaskAndFormatUtils.formatAmount
import viewmodels.checkAnswers.*
import views.html.TestOnlyPaymentPlanUpdateView

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TestOnlyAmendPaymentPlanUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: TestOnlyPaymentPlanUpdateView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    if (nddsService.amendPaymentPlanGuard(userAnswers)) {
      val currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK)
      val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

      userAnswers.get(PaymentPlanDetailsQuery) match {
        case Some(response) =>
          val planDetail = response.paymentPlanDetails
          val directDebitDetails = response.directDebitDetails
          val paymentAmount = userAnswers.get(AmendPaymentAmountPage)
          val startDate = userAnswers.get(AmendPlanStartDatePage)
          for {
            directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
            paymentPlanReference <- Future.fromTry(Try(userAnswers.get(PaymentPlanReferenceQuery).get))
          } yield {
            Ok(
              view(
                appConfig.hmrcHelplineUrl,
                currencyFormat.format(paymentAmount),
                startDate.map(dateFormat.format).getOrElse(""),
                directDebitReference,
                directDebitDetails.bankAccountName.getOrElse(""),
                directDebitDetails.bankAccountNumber.getOrElse(""),
                directDebitDetails.bankSortCode.getOrElse(""),
                planDetail.paymentReference,
                dateFormat.format(planDetail.submissionDateTime),
                routes.PaymentPlanDetailsController.onPageLoad()
              )
            )
          }
        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    } else {
      val planType = request.userAnswers.get(ManagePaymentPlanTypePage).getOrElse("")
      logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: $planType")
      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
