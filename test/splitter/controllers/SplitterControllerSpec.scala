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

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class SplitterControllerSpec extends AnyFreeSpec, Matchers, OptionValues {

  "SplitterController" - {

    def nddsFrontendUrl =
      controllers.routes.LandingController.onPageLoad().url

    "redirect on GET" - {

      "redirects /directdebits to the new service" in {
        val application =
          new GuiceApplicationBuilder()
            .build()

        running(application) {
          val result = route(application, FakeRequest(GET, "/directdebits")).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe nddsFrontendUrl
        }
      }

      "redirects /directdebits/* to the new service" in {
        val application =
          new GuiceApplicationBuilder()
            .build()

        running(application) {
          val result = route(application, FakeRequest(GET, "/directdebits/foo?bar=1")).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe nddsFrontendUrl
        }
      }
    }
  }

}
