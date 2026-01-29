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

package splitter.controllers

import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import splitter.connectors.AllowListConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SplitterControllerSpec extends AnyFreeSpec, Matchers, OptionValues {

  class FakeAllowListConnector(result: Boolean) extends AllowListConnector:
    override def check(userId: String)(using HeaderCarrier): Future[Boolean] = Future.successful(result)

  def application(connectorResponse: Boolean)(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    builder
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[AllowListConnector].toInstance(FakeAllowListConnector(connectorResponse))
      )

  private def nddsFrontendUrl = controllers.routes.LandingController.onPageLoad().url

  "redirect on GET" - {
    "for GET request to /directdebits" - {
      "the user is redirect to the legacy service when the check is false" in {
        running(application(false)) { app =>
          val result = route(app, FakeRequest("GET", "/directdebits")).value

          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result).value mustBe "/national-direct-debits"
        }
      }

      "the user is redirected to start page of the replatform service when the check is true" in {
        running(application(true)) { app =>
          val result = route(app, FakeRequest("GET", "/directdebits")).value

          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result).value mustBe nddsFrontendUrl
        }
      }
    }

    "for GET requests that are prefixed with /directdebits and have additional path components" - {
      "the user is redirect to the legacy service when the check is false" in {
        running(application(false)) { app =>
          val result = route(app, FakeRequest("GET", "/directdebits/foo?bar=1")).value

          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result).value mustBe "/national-direct-debits"
        }
      }

      "the user is redirected to start page of the replatform service when the check is true" in {
        running(application(true)) { app =>
          val result = route(app, FakeRequest("GET", "/directdebits/foo?bar=1")).value

          status(result) mustBe Status.SEE_OTHER
          redirectLocation(result).value mustBe nddsFrontendUrl
        }
      }
    }
  }
}
