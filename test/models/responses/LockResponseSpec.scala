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

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

class LockResponseSpec extends SpecBase {
  "LockResponse.lockStatus" - {
    "should be NotLocked when isLocked is false (regardless of unverifiable)" in new Setup {
      LockResponse(id, calls, isLocked = false, None, None, None, None).lockStatus mustBe NotLocked
      LockResponse(id, calls, isLocked = false, unverifiable = Some(false), None, None, None).lockStatus mustBe NotLocked
      LockResponse(id, calls, isLocked = false, unverifiable = Some(true), None, None, None).lockStatus mustBe NotLocked
    }

    "should be LockedAndVerified when isLocked is true and unverifiable is some(true)" in new Setup {
      LockResponse(id, calls, isLocked = true, unverifiable = Some(true), None, None, None).lockStatus mustBe LockedAndUnverified
    }

    "should be LockedAndVerified when isLocked is true and unverifiable is None or Some(false)" in new Setup {
      LockResponse(id, calls, isLocked = true, unverifiable = None, None, None, None).lockStatus mustBe LockedAndVerified
      LockResponse(id, calls, isLocked = true, unverifiable = Some(false), None, None, None).lockStatus mustBe LockedAndVerified
    }
  }

  "LockResponse JSON format" - {
    "should round-trip to/from JSON" in new Setup {
      val model: LockResponse = LockResponse(
        _id                   = id,
        verifyCalls           = calls,
        isLocked              = true,
        unverifiable          = Some(true),
        createdAt             = None,
        lastUpdated           = None,
        lockoutExpiryDateTime = Some(dateTime)
      )

      val json: JsValue = Json.toJson(model)
      json.as[LockResponse] mustBe model
    }
  }

  trait Setup {
    val id = "test_id"
    val calls = 3
    val dateTime: Instant = Instant.parse("2025-06-28T15:30:00z")
  }
}
