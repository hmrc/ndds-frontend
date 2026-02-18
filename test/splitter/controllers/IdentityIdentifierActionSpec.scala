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

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IdentityIdentifierActionSpec extends SpecBase {

  class Harness(authAction: IdentityIdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "IdentityIdentifierAction" - {
    "redirect to legacy service when allow list checks are disabled" in {
      val application = applicationBuilder().build()
      
      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val configuration = Configuration("features.allowListChecksEnabled" -> "false")
          .withFallback(application.injector.instanceOf[Configuration])


        val authAction =
          new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, configuration, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/national-direct-debits")
      }
    }
    
    "returns the result from the controller when" - {
      "when the user is successfully logged in" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction = new IdentityIdentifierActionImpl(
            new PassingAuthConnector(Some(Credentials("provider-id-value", "provider-type-value"))),
            appConfig,
            configuration,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val request = FakeRequest().withSession(SessionKeys.sessionId -> "Bearer Foo")
          val result = controller.onPageLoad()(request)
          
          status(result) mustBe OK
        }
      }
    }

    "must redirect the user to the unauthorised page" - {
      "the user has an unsupported affinity group" in {
        val application = applicationBuilder().build()
        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction =
            new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }

      "the user used an unaccepted auth provider" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction =
            new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }

      "the user doesn't have sufficient confidence level" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction =
            new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }

      "the user has an unsupported credential role" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction =
            new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
        }
      }
    }

    "must redirect the user to the login page" - {
      "when the user hasn't logged in" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction = new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }

      "the user's session has expired" in {
        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val configuration = application.injector.instanceOf[Configuration]

          val authAction = new IdentityIdentifierActionImpl(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, configuration, bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }
  }
}

class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}

class PassingAuthConnector[R](res: R) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.successful(res.asInstanceOf[A])
}