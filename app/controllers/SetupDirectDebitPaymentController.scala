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
import models.responses.{LockedAndUnverified, LockedAndVerified, NotLocked}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RDSDatacacheService
import services.LockService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SetupDirectDebitPaymentView

import scala.concurrent.{ExecutionContext, Future}

class SetupDirectDebitPaymentController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       rdsDatacacheService: RDSDatacacheService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: SetupDirectDebitPaymentView,
                                       lockService: LockService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      lockService.isUserLocked(request.userId) flatMap { _.lockStatus match
        case NotLocked =>
          rdsDatacacheService.retrieveAllDirectDebits(request.userId) map { rdsResponse =>
            val ddiCount = rdsResponse.directDebitCount
            Ok(view(ddiCount, routes.YourDirectDebitInstructionsController.onPageLoad()))
          }
        case LockedAndVerified => Future.successful(Redirect(routes.ReachedLimitController.onPageLoad()))
        case LockedAndUnverified => Future.successful(Redirect(routes.AccountDetailsNotVerifiedController.onPageLoad()))
      }
  }

}

