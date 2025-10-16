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

package utils

sealed trait Frequency
object Frequency {
  case object Weekly      extends Frequency
  case object Fortnightly extends Frequency
  case object FourWeekly  extends Frequency
  case object Monthly     extends Frequency
  case object Quarterly   extends Frequency
  case object SixMonthly  extends Frequency
  case object Annually    extends Frequency

  def fromString(value: String): Frequency = value.trim.toLowerCase match {
    case "weekly"      => Weekly
    case "fortnightly" => Fortnightly
    case "four weekly" => FourWeekly
    case "monthly"     => Monthly
    case "quarterly"   => Quarterly
    case "six monthly" => SixMonthly
    case "annually"    => Annually
    case other =>
      throw new IllegalArgumentException(s"Unknown payment frequency: $other")
  }
}
