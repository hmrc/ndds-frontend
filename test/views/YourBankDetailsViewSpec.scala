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

package views

import controllers.routes
import forms.YourBankDetailsFormProvider
import models.NormalMode
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.YourBankDetailsView

class YourBankDetailsViewSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  lazy val personalorBusinessRoute = routes.PersonalOrBusinessAccountController.onPageLoad(NormalMode).url

  "YourBankDetailsView" should {
    "render the page with all fields and correct maxlength attributes" in new Setup {
      val html: HtmlFormat.Appendable = view(form, NormalMode, Call("GET", personalorBusinessRoute))
      val doc = org.jsoup.Jsoup.parse(html.toString())

      doc.select("h1").text() mustBe messages("yourBankDetails.heading")
    }

    "display error messages when form has errors" in new Setup {
      val errorForm = form.withError("sortCode", "yourBankDetails.error.sortCode.tooShort")
      val html = view(errorForm, NormalMode, Call("GET", personalorBusinessRoute))
      val doc = org.jsoup.Jsoup.parse(html.toString())
      doc.select(".govuk-error-summary").text() must include(messages("yourBankDetails.error.sortCode.tooShort"))
    }
  }

  trait Setup {
    val formProvider = new YourBankDetailsFormProvider()
    val form: Form[?] = formProvider()
    implicit val request: Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(play.api.i18n.Lang.defaultLang, app.injector.instanceOf[play.api.i18n.MessagesApi])

    val view: YourBankDetailsView = app.injector.instanceOf[YourBankDetailsView]
  }
}
