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

package controllers.testOnly

import controllers.actions.*
import controllers.routes
import models.NormalMode
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TestOnlyAmendingPaymentPlanView
import config.FrontendAppConfig
import queries.PaymentPlanDetailsQuery

import javax.inject.Inject
import utils.Constants
import uk.gov.hmrc.govukfrontend.views.html.components._
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPlanStartDateSummary}

class TestOnlyAmendingPaymentPlanController @Inject()(
                                                        override val messagesApi: MessagesApi,
                                                        identify: IdentifierAction,
                                                        getData: DataRetrievalAction,
                                                        requireData: DataRequiredAction,
                                                        nddsService: NationalDirectDebitService,
                                                        val controllerComponents: MessagesControllerComponents,
                                                        testOnlyView: TestOnlyAmendingPaymentPlanView,
                                                        appConfig: FrontendAppConfig
                                                      ) extends FrontendBaseController
  with I18nSupport
  with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData){implicit request =>
    val planDetailsResponse = request.userAnswers.get(PaymentPlanDetailsQuery).getOrElse(
      throw new RuntimeException("Missing plan details")
    )

    val planDetail = planDetailsResponse.paymentPlanDetails
    val amountRow =
      AmendPaymentAmountSummary
        .row(
          planDetail.planType,
          planDetail.scheduledPaymentAmount
        )
        .copy(actions = Some(Actions(
          items = Seq(
            ActionItem(
              href = routes.AmendPaymentAmountController.onPageLoad(mode = NormalMode).url,
              content = Text("Change"),
              visuallyHiddenText = Some("payment amount")
            )
          )
        )))

    val dateRow =
      AmendPlanStartDateSummary
        .row(
          planDetail.planType,
          planDetail.scheduledPaymentStartDate,
          Constants.shortDateTimeFormatPattern
        )
        .copy(actions = Some(Actions(
          items = Seq(
            ActionItem(
              href = routes.AmendPlanStartDateController.onPageLoad(mode = NormalMode).url,
              content = Text("Change"),
              visuallyHiddenText = Some("payment date")
            )
          )
        )))

    Ok(
      testOnlyView(
        appConfig.hmrcHelplineUrl,
        "amendingPaymentPlan.p1.single",
        amountRow,
        dateRow
      )
    )
  }
}
