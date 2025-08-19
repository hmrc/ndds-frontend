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
import models.{NddDAO, NddResponse, NddDetails}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class DirectDebitCacheRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[NddDAO]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  val rdsResponse: NddResponse = NddResponse(
    directDebitCount = 3,
    directDebitList = Seq(
      NddDetails(
        ddiRefNumber = "122222",
        submissionDateTime = LocalDateTime.parse("2024-02-01T00:00:00"),
        bankSortCode = "666666",
        bankAccountNumber = "00000000",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      ),
      NddDetails(
        ddiRefNumber = "133333",
        submissionDateTime = LocalDateTime.parse("2024-03-02T00:00:00"),
        bankSortCode = "555555",
        bankAccountNumber = "11111111",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      ),
      NddDetails(
        ddiRefNumber = "144444",
        submissionDateTime = LocalDateTime.parse("2024-03-03T00:00:00"),
        bankSortCode = "333333",
        bankAccountNumber = "22222222",
        bankAccountName = "BankLtd",
        auDdisFlag = false,
        numberOfPayPlans = 0
      )
    )
  )

  def rdsData(debits: Seq[NddDetails] = Seq()): NddDAO =
    NddDAO("id", Instant.ofEpochSecond(1), debits)

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  protected override val repository: DirectDebitCacheRepository = new DirectDebitCacheRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )(scala.concurrent.ExecutionContext.Implicits.global)

  ".cacheResponse" - {

    "must set the last updated time on the cached data to `now`, and save an empty response" in {

      val expectedResult = rdsData() copy (lastUpdated = instant)

      repository.cacheResponse(NddResponse(0, Seq()))("id").futureValue
      val updatedRecord = find(Filters.equal("_id", expectedResult.id)).futureValue.headOption.value

      updatedRecord mustEqual expectedResult
    }

    "must set the last updated time on the cached data to `now`, and save an RDS response" in {

      val expectedResult = rdsData(rdsResponse.directDebitList) copy (lastUpdated = instant)

      repository.cacheResponse(rdsResponse)("id").futureValue
      val updatedRecord = find(Filters.equal("_id", expectedResult.id)).futureValue.headOption.value

      updatedRecord mustEqual expectedResult
    }

    mustPreserveMdc(repository.cacheResponse(rdsResponse)("id"))
  }

  ".retrieveCache" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        repository.cacheResponse(rdsResponse)("id").futureValue

        val result         = repository.retrieveCache("id").futureValue
        val expectedResult = rdsResponse.directDebitList

        result mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.retrieveCache("id that does not exist").futureValue must be(Seq())
      }
    }

    mustPreserveMdc(repository.retrieveCache("id"))
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {
        val data: NddDAO = rdsData()

        repository.cacheResponse(NddResponse(0, Seq()))("id").futureValue
        repository.keepAlive(data.id).futureValue

        val expected = data copy (lastUpdated = instant)
        val updatedCache = find(Filters.equal("_id", data.id)).futureValue.headOption.value

        updatedCache mustEqual expected
      }
    }

    "when there is no record for this id" - {

      "must return true" in {
        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }

    mustPreserveMdc(repository.keepAlive(rdsData().id))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      implicit lazy val ec: ExecutionContext =
        ExecutionContext.fromExecutor(new MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }.futureValue
    }
}
