/*
 * Copyright 2026 HM Revenue & Customs
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
import pages.SuspensionPeriodRangeDatePage

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants
import views.html.AlreadySuspendedErrorView

import java.time.format.DateTimeFormatter

class AlreadySuspendedErrorController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AlreadySuspendedErrorView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      implicit val messages: Messages =
        controllerComponents.messagesApi.preferred(request)

      val formatterLocale: DateTimeFormatter =
        DateTimeFormatter.ofPattern(
          Constants.longDateTimeFormatPattern,
          messages.lang.locale
        )

      val (formattedSuspensionStartDate, formattedSuspensionEndDate) =
        request.userAnswers
          .get(SuspensionPeriodRangeDatePage)
          .map { suspensionRange =>
            (
              suspensionRange.startDate.format(formatterLocale),
              suspensionRange.endDate.format(formatterLocale)
            )
          }
          .getOrElse(("", ""))

      Ok(
        view(
          formattedSuspensionStartDate,
          formattedSuspensionEndDate
        )
      )
    }

}
