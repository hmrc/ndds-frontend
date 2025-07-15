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

package models

import play.api.libs.json.{Json, OFormat}
import play.twirl.api.{Html, HtmlFormat}
import utils.Constants.EMPTY_STRING

case class UKBankAddress(
                      addressLine1: String,
                      addressLine2: Option[String],
                      addressLine3: Option[String],
                      addressLine4: Option[String],
                      addressLine5: Option[String],
                      postCode:     String
                    ) {

  private def formatLine(value: String): String =
    HtmlFormat.escape(value).body + "<br>"

  private def formatOptionalLine(opt: Option[String]): String =
    opt.map(formatLine).getOrElse(EMPTY_STRING)

  def getFullAddress: Html = {
    Html(Seq(formatLine(addressLine1) +
      formatOptionalLine(addressLine2) +
      formatOptionalLine(addressLine3) +
      formatOptionalLine(addressLine4) +
      formatOptionalLine(addressLine5) +
      formatLine(postCode)
    ).flatten.mkString)
  }
}

object UKBankAddress {
  implicit val format: OFormat[UKBankAddress] = Json.format[UKBankAddress]
}
