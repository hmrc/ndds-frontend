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

package models.responses

import play.api.libs.json.{Format, Json}

import java.time.Instant


case class LockResponse(_id: String,
                        verifyCalls: Int,
                        isLocked: Boolean,
                        unverifiable: Option[Boolean],
                        createdAt: Option[Instant],
                        lastUpdated: Option[Instant],
                        lockoutExpiryDateTime: Option[Instant]) {
  val lockStatus: LockStatus = (isLocked, unverifiable) match {
    case (false, _)         => NotLocked
    case (true, Some(true)) => LockedAndUnverified
    case (true, _)          => LockedAndVerified
  }
}

object LockResponse {
  implicit val format: Format[LockResponse] = Json.format[LockResponse]
}

sealed trait LockStatus

case object NotLocked extends LockStatus
case object LockedAndVerified extends LockStatus
case object LockedAndUnverified extends LockStatus