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

package repositories

import config.FrontendAppConfig
import models.{NddDAO, NddDetails, NddResponse}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions, Updates}
import play.api.libs.json.Format
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DirectDebitCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig,
  clock: Clock,
  crypto: Encrypter & Decrypter
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[NddDAO](
      collectionName = "direct-debit-cache",
      mongoComponent = mongoComponent,
      domainFormat   = NddDAO.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
        )
      )
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val encryption: Encrypter & Decrypter = crypto

  private def byId(id: String): Bson = Filters.equal("_id", id)

  def retrieveCache(id: String): Future[Seq[NddDetails]] = Mdc.preservingMdc {
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
        .map {
          case Some(cache) => cache.directDebits.map(_.decrypted)
          case _           => Seq()
        }
        .recover(_ => Seq())
    }
  }

  def cacheResponse(response: NddResponse)(id: String): Future[Boolean] = Mdc.preservingMdc {
    val encryptedDocument = NddDAO(id, Instant.now(clock), response.directDebitList.map(_.encrypted))

    retrieveCache(id) flatMap {
      case Seq() =>
        collection
          .replaceOne(
            filter      = byId(id),
            replacement = encryptedDocument,
            options     = ReplaceOptions().upsert(true)
          )
          .toFuture()
          .map(_ => true)
      case existingCache =>
        Future.successful(true)
    }
  }

  private[repositories] def keepAlive(id: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)
      .recover(_ => false)
  }
}
