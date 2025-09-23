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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentPlanConfirmationView

class PaymentPlanConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PaymentPlanConfirmationView
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData) {
    implicit request =>
      //TODO: Replace CheckYourAnswerPage with AP2 and take paymentAmount and startDate from there
      //val referenceNumber = request.userAnswers.get(CheckYourAnswerPage).getOrElse(throw new Exception("Missing generated DDI reference number"))
      //val paymentAmount =  request.userAnswers.get(AP2Page).getOrElse(throw new Exception("Missing payment amount"))
      //val startDate =  request.userAnswers.get(AP2Page).getOrElse(throw new Exception("Missing start date"))
      //val endDate =  request.userAnswers.get(AP2Page).getOrElse(throw new Exception("Missing end date"))
      val referenceNumber = "123456789K"
      val paymentAmount: BigDecimal = BigDecimal("1000.00")
      val formattedPaymentAmount: String = NumberFormat.getCurrencyInstance(Locale.UK).format(paymentAmount)
      val startDate: LocalDate = LocalDate.of(2025, 9, 3)
      val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      val endDate: LocalDate = LocalDate.of(2025,11,11)
      val formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
      Ok(view(referenceNumber, formattedPaymentAmount,formattedStartDate,formattedEndDate))
  }
}
