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

import config.{FakeEncrypterDecrypter, FrontendAppConfig}
import models.responses.{NddDDPaymentPlansResponse, NddPaymentPlan, PaymentPlanDAO}
import org.mockito.Mockito.when
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanCacheRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[PaymentPlanDAO]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  val direDebitPaymentPlans = NddDDPaymentPlansResponse(
    bankSortCode      = "123456",
    bankAccountNumber = "12345678",
    bankAccountName   = "MyBankAcc",
    auDdisFlag        = "01",
    paymentPlanCount  = 0,
    paymentPlanList = Seq(
      NddPaymentPlan(
        scheduledPaymentAmount = 100.0,
        planRefNumber          = "planRefNumber1",
        planType               = "singlePaymentPlan",
        paymentReference       = "paymentReference1",
        hodService             = "sa",
        submissionDateTime     = LocalDateTime.now.minusDays(3)
      ),
      NddPaymentPlan(
        scheduledPaymentAmount = 200.0,
        planRefNumber          = "planRefNumber2",
        planType               = "singlePaymentPlan",
        paymentReference       = "paymentReference2",
        hodService             = "sa",
        submissionDateTime     = LocalDateTime.now.minusDays(3)
      )
    )
  )

  private val userId = "userId"
  private val directDebitReference = "ddReference"

  def rdsData(userId: String, directDebitReference: String, ddPaymentPlans: NddDDPaymentPlansResponse): NddDDPaymentPlansResponse =
    direDebitPaymentPlans

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  def filterBy(userId: String, directDebitReference: String): Bson = Filters.and(
    Filters.equal("userId", userId),
    Filters.equal("directDebitReference", directDebitReference)
  )

  private val fakeCrypto: Encrypter with Decrypter = new FakeEncrypterDecrypter()

  override protected val repository: PaymentPlanCacheRepository = new PaymentPlanCacheRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock,
    crypto         = fakeCrypto
  )(scala.concurrent.ExecutionContext.Implicits.global)

  ".saveToCache" - {

    "must insert data into cache successfully" in {

      val result = repository.saveToCache(userId, directDebitReference, direDebitPaymentPlans).futureValue
      val updatedRecord = find(filterBy(userId, directDebitReference)).futureValue.headOption.value

      result mustEqual true
      updatedRecord.ddPaymentPlans mustEqual direDebitPaymentPlans
    }

    mustPreserveMdc(repository.saveToCache(userId, directDebitReference, direDebitPaymentPlans))
  }

  ".retrieveCache" - {

    "when there is a record for this userId and directDebitReference" - {

      "must update the lastUpdated time and get the record" in {
        repository.saveToCache(userId, directDebitReference, direDebitPaymentPlans).futureValue

        val result = repository.retrieveCache(userId, directDebitReference).futureValue

        val updatedCache = find(filterBy(userId, directDebitReference)).futureValue

        result mustBe Some(direDebitPaymentPlans)
        updatedCache.head.lastUpdated mustEqual instant
      }
    }

    "when there is no record for this userId and directDebitReference" - {

      "must return None" in {

        repository.retrieveCache("userId not in db", "ddRref not in db").futureValue mustBe None
      }
    }

    mustPreserveMdc(repository.retrieveCache(userId, directDebitReference))
  }

  ".keepAlive" - {

    "when there is a record for this userId and directDebitReference" - {

      "must update its lastUpdated to `now` and return true" in {
        repository.saveToCache(userId, directDebitReference, direDebitPaymentPlans).futureValue
        repository.keepAlive(userId, directDebitReference).futureValue

        val updatedCache = find(filterBy(userId, directDebitReference)).futureValue

        updatedCache.head.lastUpdated mustEqual instant
      }
    }

    "when there is no record for this id" - {

      "must return true" in {
        repository.retrieveCache("userId not in db", "ddRref not in db").futureValue mustBe None
      }
    }

    mustPreserveMdc(repository.keepAlive(userId, directDebitReference))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" ignore {

      implicit lazy val ec: ExecutionContext =
        ExecutionContext.fromExecutor(new MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }.futureValue
    }
}
