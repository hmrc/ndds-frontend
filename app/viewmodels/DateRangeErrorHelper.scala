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

package viewmodels

import play.api.data.Form

object DateRangeErrorHelper {

  final case class Result(form: Form[?], errorLinkOverrides: Map[String, String])

  def collapseErrors(form: Form[?], dateFieldKeys: Seq[String]): Result = {
    val dateFieldParts = Seq("day", "month", "year")

    def normalise(key: String): String =
      dateFieldKeys.find(field => key == field || key.startsWith(s"$field.")).getOrElse(key)

    val dateFieldData = dateFieldKeys.map { baseKey =>
      val related = form.errors.filter(error => normalise(error.key) == baseKey)
      val collapsedError = related.find(_.key == baseKey).orElse(related.headOption).map(_.copy(key = baseKey))
      val anchor = related.collectFirst {
        case error if error.key == baseKey => baseKey
        case error if dateFieldParts.exists(part => error.key == s"$baseKey.$part") => error.key
      }
      baseKey -> (collapsedError, anchor)
    }.toMap

    val collapsedDateErrors = dateFieldData.values.flatMap(_._1).toSeq

    val remainingErrors = form.errors.filterNot(error => dateFieldKeys.exists(baseKey => normalise(error.key) == baseKey))

    val errorLinkOverrides = dateFieldData.flatMap {
      case (baseKey, (_, Some(anchor))) if anchor != baseKey => Some(baseKey -> anchor)
      case _ => None
    }

    Result(form.copy(errors = collapsedDateErrors ++ remainingErrors), errorLinkOverrides)
  }
}

