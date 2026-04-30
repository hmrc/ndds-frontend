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

package testOnly.controllers

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

import config.FrontendAppConfig
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.utils.TestOnlyClockProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import java.time.{Clock, Instant, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class ClockController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  clockProvider: TestOnlyClockProvider,
  clockPage: testOnly.views.html.ClockSettings
)(using
  ExecutionContext,
  FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  val onPageLoad: Action[AnyContent] = Action { implicit request =>
    Ok(clockPage(clockProvider.clock, ClockController.newClockForm))
  }

  val submitNewTime: Action[AnyContent] = Action { implicit request =>
    ClockController.newClockForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(clockPage(clockProvider.clock, formWithErrors)),
        clockSettings => {
          clockProvider.setClock(Clock.fixed(clockSettings.instant, clockSettings.zone))
          Redirect(routes.ClockController.onPageLoad)
        }
      )
  }

  val submitResetTime: Action[AnyContent] = Action { implicit request =>
    clockProvider.resetClock()
    Redirect(routes.ClockController.onPageLoad)
  }
}

object ClockController {

  final case class ClockSettingsForm(instant: Instant, zone: ZoneId)

  val newClockForm: Form[ClockSettingsForm] = Form(
    mapping(
      "instant" -> nonEmptyText
        .verifying("Could not parse instant", s => Try(Instant.parse(s)).isSuccess)
        .transform[Instant](Instant.parse, _.toString),
      "zone" -> nonEmptyText
        .verifying("Could not parse zone", s => Try(ZoneId.of(s)).isSuccess)
        .transform[ZoneId](ZoneId.of, _.getId)
    )(ClockSettingsForm.apply)(p => Some(Tuple.fromProductTyped(p)))
  )

}
