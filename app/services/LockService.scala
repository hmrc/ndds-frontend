package services

import config.FrontendAppConfig
import connectors.LockConnector
import models.responses.LockResponse
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LockService @Inject()(
                             lockConnector: LockConnector,
                             config: FrontendAppConfig
                           ) {

  private val defaultLockResponse: LockResponse = LockResponse(
    _id = "",
    verifyCalls = 0,
    isLocked = false,
    unverifiable = None,
    createdAt = None,
    lastUpdated = None,
    lockoutExpiryDateTime = Some(Instant.parse("2025-06-28T15:30:30Z"))
  )

  def isUserLocked(credId: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[LockResponse] =
    stubLockIfFeatureDisabled(lockConnector.checkLock(credId))

  def updateLockForUser(credId: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[LockResponse] =
    stubLockIfFeatureDisabled(lockConnector.updateLock(credId))

  def markUserAsUnverifiable(credId: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[LockResponse] =
    stubLockIfFeatureDisabled(lockConnector.markUnverifiable(credId))

  private def stubLockIfFeatureDisabled(
                                         f: HeaderCarrier => Future[LockResponse]
                                       )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LockResponse] = {

    if (config.isLockServiceEnabled) {
      f(hc).recover {
        case e: UpstreamErrorResponse if e.statusCode == 409 =>
          defaultLockResponse
      }
    } else {
      Future.successful(defaultLockResponse)
    }
  }
}
