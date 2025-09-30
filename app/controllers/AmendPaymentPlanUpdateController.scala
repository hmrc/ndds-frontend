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
import pages.*
import play.api.Logging

import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PaymentReferenceQuery
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AmendPaymentPlanUpdateView

import scala.concurrent.Future

class AmendPaymentPlanUpdateController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       nddsService: NationalDirectDebitService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: AmendPaymentPlanUpdateView
                                     )
  extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val userAnswers = request.userAnswers
      if (nddsService.amendPaymentPlanGuard(userAnswers)) {
        val paymentReference = userAnswers.get(PaymentReferenceQuery).getOrElse("")
        val paymentAmount = userAnswers.get(AmendPaymentAmountPage).getOrElse(BigDecimal(0))
        val formattedRegPaymentAmount: String = NumberFormat.getCurrencyInstance(Locale.UK).format(paymentAmount)
        val startDate = userAnswers.get(AmendPlanStartDatePage).get
        val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val endDate = userAnswers.get(AmendPlanEndDatePage).get
        val formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        Future.successful(Ok(view(paymentReference, formattedRegPaymentAmount, formattedStartDate, formattedEndDate)))
      } else {
        val paymentPlanType = userAnswers.get(AmendPaymentPlanTypePage).getOrElse("Missing plan type from user answers")
        logger.error(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${paymentPlanType}")
        throw new Exception(s"NDDS Payment Plan Guard: Cannot amend this plan type: ${paymentPlanType}")
      }
  }
}
