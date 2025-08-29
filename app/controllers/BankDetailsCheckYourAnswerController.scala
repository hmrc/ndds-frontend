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
import forms.BankDetailsCheckYourAnswerFormProvider
import models.Mode
import models.audits.ConfirmBankDetails
import pages.BankDetailsCheckYourAnswerPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import services.AuditService
import viewmodels.govuk.all.SummaryListViewModel
import views.html.BankDetailsCheckYourAnswerView

import javax.inject.Inject

class BankDetailsCheckYourAnswerController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      identify: IdentifierAction,
                                                      getData: DataRetrievalAction,
                                                      requireData: DataRequiredAction,
                                                      auditService: AuditService,
                                                      formProvider: BankDetailsCheckYourAnswerFormProvider,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: BankDetailsCheckYourAnswerView
                                                    ) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(BankDetailsCheckYourAnswerPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      //TODO: will be covered in the next page of confirmation
      auditService.sendEvent(ConfirmBankDetails())
      val summaryList = buildSummaryList(request.userAnswers)
      Ok(view(preparedForm, mode, summaryList, routes.YourBankDetailsController.onPageLoad(mode)))
  }

  private def buildSummaryList(answers: models.UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        YourBankDetailsAccountHolderNameSummary.row(answers),
        YourBankDetailsAccountNumberSummary.row(answers),
        YourBankDetailsSortCodeSummary.row(answers),
        YourBankDetailsNameSummary.row(answers),
        YourBankDetailsAddressSummary.row(answers)
      ).flatten
    )

}
