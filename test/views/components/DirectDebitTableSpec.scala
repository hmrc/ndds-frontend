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

package views.components

import base.SpecBase
import models.DirectDebitDetails
import org.scalatest.matchers.must.Matchers
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTable
import views.html.components.DirectDebitTable

class DirectDebitTableSpec extends SpecBase with Matchers {

  "DirectDebitTable" - {

    "must render a table with headers and rows" in {
      val component = new DirectDebitTable(new GovukTable())
      val rows = Seq(
        DirectDebitDetails("DD001", "1 Jan 2025", "123456", "12345678", "0"),
        DirectDebitDetails("DD002", "2 Jan 2025", "123456", "87654321", "2")
      )

      val html = component.apply(rows)(fakeRequest, messages).toString

      html must include(messages("yourDirectDebitInstructions.dl.direct.debit.reference"))
      html must include(messages("yourDirectDebitInstructions.dl.account.number"))
      html must include(messages("yourDirectDebitInstructions.dl.date.setup"))
      html must include(messages("yourDirectDebitInstructions.dl.payment.plans"))

      html must include(routes.DirectDebitSummaryController.onRedirect("DD001").url)
      html must include(routes.DirectDebitSummaryController.onRedirect("DD002").url)
    }
  }
}
