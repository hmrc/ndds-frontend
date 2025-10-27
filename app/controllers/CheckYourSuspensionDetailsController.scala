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
import models.Mode
import navigation.Navigator
import pages.{ManagePaymentPlanTypePage, SuspensionDetailsCheckYourAnswerPage}
import play.api.Logging
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.CheckYourSuspensionDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourSuspensionDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  nddsService: NationalDirectDebitService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourSuspensionDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    logger.info("Display suspension details confirmation page")
    val userAnswers = request.userAnswers

    if (nddsService.suspendPaymentPlanGuard(userAnswers)) {
      val summaryList = buildSummaryList(userAnswers)
      Ok(view(summaryList, mode, routes.SuspensionPeriodRangeDateController.onPageLoad(mode)))
    } else {
      logger.error(
        s"NDDS Payment Plan Guard: Cannot carry out suspension functionality for this plan type: ${userAnswers.get(ManagePaymentPlanTypePage)}"
      )
      Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val confirmed = true

      for {
        updatedAnswers <- Future.fromTry(
                            request.userAnswers
                              .set(SuspensionDetailsCheckYourAnswerPage, confirmed)
                          )
        _ <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(SuspensionDetailsCheckYourAnswerPage, mode, updatedAnswers))

    }

  private def buildSummaryList(answers: models.UserAnswers)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        SuspensionPeriodRangeDateSummary.row(answers, true)
      ).flatten
    )

}
