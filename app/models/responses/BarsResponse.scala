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

package models.responses

import models.responses.BarsResponse.*
import play.api.libs.json.*

enum BarsResponse {
  case Yes, No, Partial, Indeterminate, Inapplicable, Error
}

given Format[BarsResponse] with {

  def reads(json: JsValue): JsResult[BarsResponse] = json match {
    case JsString(str) =>
      str.toLowerCase match {
        case "yes"           => JsSuccess(Yes)
        case "no"            => JsSuccess(No)
        case "partial"       => JsSuccess(Partial)
        case "indeterminate" => JsSuccess(Indeterminate)
        case "inapplicable"  => JsSuccess(Inapplicable)
        case "error"         => JsSuccess(Error)
        case _               => JsError(s"Unknown value: $str")
      }

    case _ => JsError("Expected String")

  }

  def writes(barsResponse: BarsResponse): JsValue = {
    JsString(barsResponse.toString.toLowerCase())
  }
}
