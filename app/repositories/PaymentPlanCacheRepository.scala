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
import models.responses.{NddDDPaymentPlansResponse, PaymentPlanDAO}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
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
class PaymentPlanCacheRepository @Inject() (mongoComponent: MongoComponent,
                                            appConfig: FrontendAppConfig,
                                            clock: Clock,
                                            crypto: Encrypter & Decrypter
                                           )(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[PaymentPlanDAO](
      collectionName = "payment-plans-cache",
      mongoComponent = mongoComponent,
      domainFormat   = PaymentPlanDAO.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("userId"),
            Indexes.ascending("directDebitReference")
          ),
          IndexOptions()
            .name("userIdDirectDebitRefIdx")
            .unique(true)
        )
      )
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val encryption: Encrypter & Decrypter = crypto

  def saveToCache(userId: String, directDebitReference: String, ddPaymentPlans: NddDDPaymentPlansResponse): Future[Boolean] = Mdc.preservingMdc {
    val encryptedDocument =
      PaymentPlanDAO(userId = userId, directDebitReference: String, lastUpdated = Instant.now(clock), ddPaymentPlans = ddPaymentPlans.encrypted)

    retrieveCache(userId, directDebitReference) flatMap {
      case None =>
        collection
          .replaceOne(
            filter      = filterBy(userId, directDebitReference),
            replacement = encryptedDocument,
            options     = ReplaceOptions().upsert(true)
          )
          .toFuture()
          .map(_ => true)
      case existingCache =>
        Future.successful(true)
    }
  }

  def retrieveCache(userId: String, directDebitReference: String): Future[Option[NddDDPaymentPlansResponse]] = Mdc.preservingMdc {
    keepAlive(userId, directDebitReference).flatMap { _ =>
      collection
        .find(filterBy(userId, directDebitReference))
        .headOption()
        .map {
          case Some(cache) => Some(cache.ddPaymentPlans.decrypted)
          case _           => None
        }
        .recover(_ => None)
    }
  }

  private def filterBy(userId: String, directDebitReference: String): Bson = Filters.and(
    Filters.equal("userId", userId),
    Filters.equal("directDebitReference", directDebitReference)
  )

  private[repositories] def keepAlive(userId: String, directDebitReference: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .updateOne(
        filter = filterBy(userId, directDebitReference),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)
      .recover(_ => false)
  }
}
