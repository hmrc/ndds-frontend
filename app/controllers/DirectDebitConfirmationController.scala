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
import pages.{CheckYourAnswerPage, PaymentDatePage, PaymentReferencePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlanReferenceQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{DateSetupSummary, PaymentAmountSummary, PaymentDateSummary, YourBankDetailsAccountHolderNameSummary, YourBankDetailsAccountNumberSummary, YourBankDetailsSortCodeSummary}
import viewmodels.govuk.all.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import views.html.DirectDebitConfirmationView

import javax.inject.Inject

class DirectDebitConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: DirectDebitConfirmationView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val referenceNumber = request.userAnswers
      .get(CheckYourAnswerPage)
      .getOrElse(throw new Exception("Missing generated DDI reference number"))

    val directDebitDetails = SummaryListViewModel(
      rows = Seq(
        Option(
          SummaryListRowViewModel(
            key     = Key(Text("Direct Debit reference")),
            value   = ValueViewModel(Text(referenceNumber.ddiRefNumber)),
            actions = Seq.empty
          )
        ),
        YourBankDetailsAccountHolderNameSummary.row(request.userAnswers).map(_.copy(actions = None)),
        YourBankDetailsSortCodeSummary.row(request.userAnswers).map(_.copy(actions = None)),
        YourBankDetailsAccountNumberSummary.row(request.userAnswers).map(_.copy(actions = None))
      ).flatten
    )

    val paymentPlanDetails = SummaryListViewModel(
      rows = Seq(
        Some(
          SummaryListRowViewModel(
            key = Key(Text("Payment reference")),
            value = ValueViewModel(
              Text(
                request.userAnswers
                  .get(PaymentReferencePage)
                  .getOrElse("Missing payment reference")
              )
            ),
            actions = Seq.empty
          )
        ),
        PaymentAmountSummary.row(request.userAnswers).map(_.copy(actions = None)),
        PaymentDateSummary.row(request.userAnswers).map(_.copy(actions = None))
      ).flatten
    )

    Ok(
      view(
        appConfig.hmrcHelplineUrl,
        referenceNumber.ddiRefNumber,
        directDebitDetails,
        paymentPlanDetails
      )
    )
  }
}
