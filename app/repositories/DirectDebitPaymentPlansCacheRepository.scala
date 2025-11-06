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
import models.responses.{NddPaymentPlan, PaymentPlanDAO}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.Format
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DirectDebitPaymentPlansCacheRepository @Inject() (mongoComponent: MongoComponent, appConfig: FrontendAppConfig, clock: Clock)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[PaymentPlanDAO](
      collectionName = "direct-debit-payment-plans-cache",
      mongoComponent = mongoComponent,
      domainFormat   = PaymentPlanDAO.format,
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

  private def byDdReference(ddReference: String): Bson = Filters.equal("_id", ddReference)

  private def retrieveCache(ddReference: String): Future[Seq[NddPaymentPlan]] = Mdc.preservingMdc {
    keepAlive(ddReference).flatMap { _ =>
      collection
        .find(byDdReference(ddReference))
        .headOption()
        .map {
          case Some(cache) => cache.paymentPlans
          case _           => Seq()
        }
        .recover(_ => Seq())
    }
  }

  def cacheResponse(ddReference: String, paymentPlans: Seq[NddPaymentPlan])(id: String): Future[Boolean] = Mdc.preservingMdc {
    val document = PaymentPlanDAO(id, Instant.now(clock), paymentPlans)

    retrieveCache(ddReference) flatMap {
      case Seq() =>
        collection
          .replaceOne(
            filter      = byDdReference(id),
            replacement = document,
            options     = ReplaceOptions().upsert(true)
          )
          .toFuture()
          .map(_ => true)
      case existingCache =>
        Future.successful(true)
    }
  }

  private[repositories] def keepAlive(ddReference: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .updateOne(
        filter = byDdReference(ddReference),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)
      .recover(_ => false)
  }
}
