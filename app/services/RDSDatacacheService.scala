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
import models.RDSDatacacheResponse
import repositories.DirectDebitCacheRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RDSDatacacheService @Inject()(rdsConnector: RDSDatacacheProxyConnector,
                                    val directDebitCache: DirectDebitCacheRepository)
                                   (implicit ec: ExecutionContext) {

  def retrieveAllDirectDebits(id: String)(implicit hc: HeaderCarrier): Future[RDSDatacacheResponse] = {
    directDebitCache.retrieveCache(id) flatMap {
      case Seq() =>
        for {
          directDebits <- rdsConnector.retrieveDirectDebits()
          _ <- directDebitCache.cacheResponse(directDebits)(id)
        } yield directDebits
      case existingCache =>
        Future.successful(RDSDatacacheResponse(existingCache.size, existingCache))
    }
  }
}