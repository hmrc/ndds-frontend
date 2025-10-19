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
import models.UserAnswers
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentPlansCountQuery}
import repositories.SessionRepository
import services.{NationalDirectDebitService, PaginationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YourDirectDebitInstructionsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourDirectDebitInstructionsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: YourDirectDebitInstructionsView,
  appConfig: FrontendAppConfig,
  nddService: NationalDirectDebitService,
  paginationService: PaginationService,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    val currentPage = request.getQueryString("page").flatMap(_.toIntOption).getOrElse(1)

    val cleansedAnswersFut = for {
      updatedAnswers <- Future.fromTry(userAnswers.remove(AmendPaymentPlanConfirmationPage))
      updatedAnswers <- Future.fromTry(updatedAnswers.remove(CreateConfirmationPage))
      updatedAnswers <- Future.fromTry(updatedAnswers.remove(SuspensionDetailsCheckYourAnswerPage))
      updatedAnswers <- Future.fromTry(updatedAnswers.remove(RemovingThisSuspensionPage))
      updatedAnswers <- Future.fromTry(updatedAnswers.remove(CancelPaymentPlanConfirmationPage))
    } yield updatedAnswers

    cleansedAnswersFut.flatMap { cleansedAnswers =>
      cleanseDirectDebitReference(cleansedAnswers).flatMap { _ =>
        nddService.retrieveAllDirectDebits(request.userId).map { directDebitDetailsData =>
          val maxLimitReached = directDebitDetailsData.directDebitCount > appConfig.maxNumberDDIsAllowed

          val paginationResult = paginationService.paginateDirectDebits(
            allDirectDebits = directDebitDetailsData.directDebitList,
            currentPage     = currentPage,
            baseUrl         = routes.YourDirectDebitInstructionsController.onPageLoad().url
          )

          Ok(
            view(
              directDebitDetails  = paginationResult.paginatedData,
              maxLimitReached     = maxLimitReached,
              paginationViewModel = paginationResult.paginationViewModel
            )
          )
        }
      }
    }
  }

  private def cleanseDirectDebitReference(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      updatedUserAnswers <- Future.fromTry(userAnswers.remove(DirectDebitReferenceQuery))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(PaymentPlansCountQuery))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers
}
