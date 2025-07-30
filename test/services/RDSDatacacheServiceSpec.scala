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

package services

import connectors.RDSDatacacheProxyConnector
import models.{RDSDatacacheResponse, RDSDirectDebitDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DirectDebitDetailsData

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.global

class RDSDatacacheServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with DirectDebitDetailsData {

  implicit val ec: ExecutionContext = global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: RDSDatacacheProxyConnector = mock[RDSDatacacheProxyConnector]
  val mockCache: DirectDebitCacheRepository = mock[DirectDebitCacheRepository]

  val service = new RDSDatacacheService(mockConnector, mockCache)(ec)

  val testId = "id"

  "RDSDatacacheService" - {
    "should retrieve existing details from Cache first" in {
      when(mockCache.retrieveCache(any()))
        .thenReturn(Future.successful(rdsResponse.directDebitList))

      val result = service.retrieveAllDirectDebits(testId).futureValue
      result mustEqual rdsResponse
    }

    "should retrieve details from Connector if Cache is empty, and cache the response" in {
      when(mockCache.retrieveCache(any()))
        .thenReturn(Future.successful(Seq.empty[RDSDirectDebitDetails]))
      when(mockConnector.retrieveDirectDebits()(any()))
        .thenReturn(Future.successful(rdsResponse))
      when(mockCache.cacheResponse(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.retrieveAllDirectDebits(testId).futureValue
      result mustEqual rdsResponse
    }

    "should be able to return no details from Connector or Cache is correctly empty" in {
      when(mockCache.retrieveCache(any()))
        .thenReturn(Future.successful(Seq.empty[RDSDirectDebitDetails]))
      when(mockConnector.retrieveDirectDebits()(any()))
        .thenReturn(Future.successful(RDSDatacacheResponse(0, Seq())))
      when(mockCache.cacheResponse(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.retrieveAllDirectDebits(testId).futureValue
      result mustEqual RDSDatacacheResponse(0, Seq())
    }
  }

}
