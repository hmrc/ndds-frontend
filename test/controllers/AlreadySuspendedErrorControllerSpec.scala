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

import base.SpecBase
import models.SuspensionPeriodRange
import pages.SuspensionPeriodRangeDatePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.Constants
import views.html.AlreadySuspendedErrorView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AlreadySuspendedErrorControllerSpec extends SpecBase {

  "AlreadySuspendedError Controller" - {

    "must return OK and the correct view for a GET" in {

      val startDate = LocalDate.of(2024, 1, 1)
      val endDate = LocalDate.of(2024, 3, 31)

      val userAnswers =
        emptyUserAnswers
          .set(
            SuspensionPeriodRangeDatePage,
            SuspensionPeriodRange(startDate, endDate)
          )
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.AlreadySuspendedErrorController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AlreadySuspendedErrorView]

        val formatter =
          DateTimeFormatter.ofPattern(
            Constants.longDateTimeFormatPattern,
            messages(application).lang.locale
          )

        val formattedStartDate = startDate.format(formatter)
        val formattedEndDate = endDate.format(formatter)

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(formattedStartDate, formattedEndDate)(
            request,
            messages(application)
          ).toString
      }
    }
  }
}
