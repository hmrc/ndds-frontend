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

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.AmendingPaymentPlanView
import pages.ManagePaymentPlanTypePage
import models.PaymentPlanType

class AmendingPaymentPlanControllerSpec extends SpecBase {

  "AmendingPaymentPlan Controller" - {

    "must return OK and the correct view for a GET" in {

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AmendingPaymentPlanController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendingPaymentPlanView]
        val appConfig = application.injector.instanceOf[config.FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(appConfig.hmrcHelplineUrl, "amendingPaymentPlan.p1.single")(request, messages(application)).toString
      }
    }

    "must show budget-specific paragraph when plan type is BudgetPaymentPlan" in {

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AmendingPaymentPlanController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendingPaymentPlanView]
        val appConfig = application.injector.instanceOf[config.FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(appConfig.hmrcHelplineUrl, "amendingPaymentPlan.p1.budget")(request, messages(application)).toString
      }
    }

    "must show single-specific paragraph when plan type is SinglePaymentPlan" in {

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.SinglePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AmendingPaymentPlanController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AmendingPaymentPlanView]
        val appConfig = application.injector.instanceOf[config.FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(appConfig.hmrcHelplineUrl, "amendingPaymentPlan.p1.single")(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecovery when guard fails" in {

      val ua = emptyUserAnswers
        .set(ManagePaymentPlanTypePage, PaymentPlanType.VariablePaymentPlan.toString)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.AmendingPaymentPlanController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
