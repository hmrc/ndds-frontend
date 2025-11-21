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

package viewmodels.checkAnswers

import base.SpecBase
import models.{CheckMode, UserAnswers, YearEndAndMonth}
import pages.YearEndAndMonthPage
import play.api.test.FakeRequest

class YearEndAndMonthSummarySpec extends SpecBase {

  "YearEndAndMonthSummary" - {

    "must display the date in YYYY MM format" in new Setup {
      val date = YearEndAndMonth(2025, 1)
      val userAnswers = UserAnswers("id").set(YearEndAndMonthPage, date).success.value

      val result = YearEndAndMonthSummary.row(userAnswers)(messages)

      result mustBe defined
      result.value.value.content.asHtml.toString must include("2025 01")
    }

    "must return None when no date is set" in new Setup {
      val userAnswers = UserAnswers("id")

      val result = YearEndAndMonthSummary.row(userAnswers)(messages)

      result mustBe None
    }

    "must have correct key" in new Setup {
      val date = YearEndAndMonth(2025, 1)
      val userAnswers = UserAnswers("id").set(YearEndAndMonthPage, date).success.value

      val result = YearEndAndMonthSummary.row(userAnswers)(messages)

      result mustBe defined
      result.value.key.content.asHtml.toString must include("Year end and month")
    }

    "must have change action with correct URL" in new Setup {
      val date = YearEndAndMonth(2025, 1)
      val userAnswers = UserAnswers("id").set(YearEndAndMonthPage, date).success.value

      val result = YearEndAndMonthSummary.row(userAnswers)(messages)

      result mustBe defined
      result.value.actions mustBe defined
      result.value.actions.value.items                              must have length 1
      result.value.actions.value.items.head.content.asHtml.toString must include("Change")
      result.value.actions.value.items.head.href mustBe controllers.routes.YearEndAndMonthController.onPageLoad(CheckMode).url
    }

    "must have visually hidden text for change action" in new Setup {
      val date = YearEndAndMonth(2025, 1)
      val userAnswers = UserAnswers("id").set(YearEndAndMonthPage, date).success.value

      val result = YearEndAndMonthSummary.row(userAnswers)(messages)

      result mustBe defined
      result.value.actions mustBe defined
      result.value.actions.value.items.head.visuallyHiddenText mustBe Some("Year end and month")
    }

    "must format specific dates correctly" in new Setup {
      val testCases = Seq(
        (YearEndAndMonth(2025, 1), "2025 01"),
        (YearEndAndMonth(2023, 12), "2023 12"),
        (YearEndAndMonth(2020, 6), "2020 06")
      )

      testCases.foreach { case (date, expectedFormat) =>
        val userAnswers = UserAnswers("id").set(YearEndAndMonthPage, date).success.value

        val result = YearEndAndMonthSummary.row(userAnswers)(messages)

        result mustBe defined
        result.value.value.content.asHtml.toString must include(expectedFormat)
      }
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: play.api.i18n.Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
