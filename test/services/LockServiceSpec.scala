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

package services

import config.FrontendAppConfig
import connectors.LockConnector
import models.responses.LockResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.Instant
import scala.concurrent.Future

class LockServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: LockConnector = mock[LockConnector]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  val service = new LockService(mockConnector, mockConfig)
  val credId = "test-cred-id"
  val lockResponse = LockResponse(
    _id                   = "lock-id-1",
    verifyCalls           = 2,
    isLocked              = true,
    unverifiable          = Some(true),
    createdAt             = Some(Instant.parse("2024-01-01T12:00:00Z")),
    lastUpdated           = Some(Instant.parse("2024-01-02T12:00:00Z")),
    lockoutExpiryDateTime = Some(Instant.parse("2024-01-10T12:00:00Z"))
  )

  val defaultLockResponse = LockResponse(
    _id                   = "",
    verifyCalls           = 0,
    isLocked              = false,
    unverifiable          = None,
    createdAt             = None,
    lastUpdated           = None,
    lockoutExpiryDateTime = Some(Instant.parse("2025-06-28T15:30:30Z"))
  )

  "LockService" should {

    "return LockResponse from isUserLocked when feature enabled" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(true)
      when(mockConnector.checkLock(any())(any())).thenReturn(Future.successful(lockResponse))

      service.isUserLocked(credId).map { result =>
        result shouldBe lockResponse
      }
    }

    "return LockResponse from updateLockForUser when feature enabled" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(true)
      when(mockConnector.updateLock(any())(any())).thenReturn(Future.successful(lockResponse))

      service.updateLockForUser(credId).map { result =>
        result shouldBe lockResponse
      }
    }

    "return LockResponse from markUserAsUnverifiable when feature enabled" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(true)
      when(mockConnector.markUnverifiable(any())(any())).thenReturn(Future.successful(lockResponse))

      service.markUserAsUnverifiable(credId).map { result =>
        result shouldBe lockResponse
      }
    }

    "return defaultLockResponse when feature disabled" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(false)

      service.isUserLocked(credId).map { result =>
        result shouldBe defaultLockResponse
      }
      service.updateLockForUser(credId).map { result =>
        result shouldBe defaultLockResponse
      }
      service.markUserAsUnverifiable(credId).map { result =>
        result shouldBe defaultLockResponse
      }
    }

    "return defaultLockResponse when connector returns 409 UpstreamErrorResponse" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(true)
      val ex = UpstreamErrorResponse("Conflict", 409)
      when(mockConnector.checkLock(any())(any())).thenReturn(Future.failed(ex))

      service.isUserLocked(credId).map { result =>
        result shouldBe defaultLockResponse
      }
    }

    "fail when connector returns non-409 UpstreamErrorResponse" in {
      when(mockConfig.isLockServiceEnabled).thenReturn(true)
      val ex = UpstreamErrorResponse("Internal Server Error", 500)
      when(mockConnector.checkLock(any())(any())).thenReturn(Future.failed(ex))

      recoverToExceptionIf[UpstreamErrorResponse] {
        service.isUserLocked(credId)
      }.map { err =>
        err shouldBe ex
      }
    }
  }
}
