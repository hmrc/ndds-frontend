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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YourDirectDebitInstructionsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourDirectDebitInstructionsController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: YourDirectDebitInstructionsView,
                                                       appConfig: FrontendAppConfig,
                                                       nddService: NationalDirectDebitService,
                                                       sessionRepository: SessionRepository
                                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    cleanseDirectDebitReference(userAnswers).flatMap { _ =>
      nddService.retrieveAllDirectDebits(request.userId) map {
        directDebitDetailsData =>
          val maxLimitReached = directDebitDetailsData.directDebitCount > appConfig.maxNumberDDIsAllowed
          Ok(view(directDebitDetailsData.directDebitList.map(_.toDirectDebitDetails), maxLimitReached))
      }
    }
  }

  private def cleanseDirectDebitReference(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      userAnswersWithoutDirectDebitReference <- Future.fromTry(userAnswers.remove(DirectDebitReferenceQuery))
      userAnswersPaymentReference <- Future.fromTry(userAnswersWithoutDirectDebitReference.remove(PaymentReferenceQuery))
      _                                     <- sessionRepository.set(userAnswersPaymentReference)
    } yield userAnswersPaymentReference
}
