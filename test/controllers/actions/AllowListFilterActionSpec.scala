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

package controllers.actions

import base.SpecBase
import models.requests.IdentifierRequest
import play.api.Configuration
import play.api.mvc.Results
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import splitter.connectors.FakeAllowListConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AllowListFilterActionSpec extends SpecBase {

  val fakeLegacyPath = "/test-url"

  "IdentityIdentifierAction" - {
    "will redirect to when allow list checks are false" in {
      val filter = AllowListFilterAction(
        FakeAllowListConnector(false),
        Configuration("microservice.services.ndds-legacy.path" -> fakeLegacyPath)
      )

      val request = IdentifierRequest(FakeRequest().withBody(""), "")
      val result = filter.invokeBlock(request, _ => Future.successful(Results.Ok("from block")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(fakeLegacyPath)
    }

    "will redirect to when allow list checks fail" in {
      val filter = AllowListFilterAction(
        FakeAllowListConnector(new Exception("bang")),
        Configuration("microservice.services.ndds-legacy.path" -> fakeLegacyPath)
      )

      val request = IdentifierRequest(FakeRequest().withBody(""), "")
      val result = filter.invokeBlock(request, _ => Future.successful(Results.Ok("from block")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(fakeLegacyPath)
    }

    "No redirect when the check return true" in {
      val filter = AllowListFilterAction(
        FakeAllowListConnector(true),
        Configuration("microservice.services.ndds-legacy.path" -> fakeLegacyPath)
      )

      val request = IdentifierRequest(FakeRequest().withBody(""), "")
      val result = filter.invokeBlock(request, _ => Future.successful(Results.Ok("from block")))

      status(result) mustBe OK
      redirectLocation(result) mustBe None
      contentAsString(result) mustEqual "from block"
    }

  }
}
