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

package controllers

import base.SpecBase
import forms.YourBankDetailsFormProvider
import models.responses.*
import models.{NormalMode, PersonalOrBusinessAccount, UserAnswers, YourBankDetails}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{PersonalOrBusinessAccountPage, YourBankDetailsPage}
import play.api.Application
import play.api.data.Form
import play.api.http.{HttpFilters, NoHttpFilters}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{BarsService, LockService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import views.html.YourBankDetailsView

import java.time.Instant
import scala.concurrent.Future

class YourBankDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new YourBankDetailsFormProvider()
  val form: Form[YourBankDetails] = formProvider()

  lazy val yourBankDetailsRoute: String = routes.YourBankDetailsController.onPageLoad(NormalMode).url
  lazy val personalOrBusinessAccountRoute: String = routes.PersonalOrBusinessAccountController.onPageLoad(NormalMode).url

  val userAnswers: UserAnswers = UserAnswers(
    userAnswersId,
    Json.obj(
      YourBankDetailsPage.toString -> Json.obj(
        "accountHolderName" -> "value 1",
        "sortCode"          -> "123212",
        "accountNumber"     -> "34211234",
        "auddisStatus"      -> true,
        "accountVerified"   -> false
      )
    )
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "YourBankDetails Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, yourBankDetailsRoute)
        val view = application.injector.instanceOf[YourBankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Call("GET", personalOrBusinessAccountRoute))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when data exists" in {
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, yourBankDetailsRoute)
        val view = application.injector.instanceOf[YourBankDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(YourBankDetails("value 1", "123212", "34211234")),
                                               NormalMode,
                                               Call("GET", personalOrBusinessAccountRoute)
                                              )(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val ua = emptyUserAnswers
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "",
            "sortCode"          -> "12",
            "accountNumber"     -> "abcd"
          )

        val boundForm = form.bind(
          Map(
            "accountHolderName" -> "",
            "sortCode"          -> "12",
            "accountNumber"     -> "abcd"
          )
        )

        val view = application.injector.instanceOf[YourBankDetailsView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          Call("GET", personalOrBusinessAccountRoute)
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, yourBankDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode"          -> "123454",
            "accountNumber"     -> "34211234",
            "auddisStatus"      -> "true"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must call BARService and handle response on valid POST submission for auddis flag true" in {
      val ua = userAnswers
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

      val mockSessionRepository = mock[SessionRepository]
      val mockBarService = mock[BarsService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val barResponse = BarsVerificationResponse(
        accountNumberIsWellFormatted             = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD                 = BarsResponse.Yes,
        sortCodeBankName                         = Some("Test Bank"),
        accountExists                            = BarsResponse.Yes,
        nameMatches                              = BarsResponse.Yes,
        sortCodeSupportsDirectDebit              = BarsResponse.Yes,
        sortCodeSupportsDirectCredit             = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = Some(BarsResponse.No),
        iban                                     = Some("GB29NWBK60161331926819"),
        accountName                              = Some("John Doe")
      )

      val bank = Bank(
        bankName       = "Test Bank",
        ddiVoucherFlag = Some("N"),
        address = BankAddress(
          lines    = Seq("1 Bank Street"),
          town     = Some("London"),
          country  = Country("UK"),
          postCode = Some("EC1A 1AA")
        )
      )

      when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right((barResponse, bank))))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BarsService].toInstance(mockBarService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode"          -> "123212",
            "accountNumber"     -> "34211234",
            "auddisStatus"      -> "true"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must call BARService and handle response on valid POST submission for auddis flag false" in {
      val ua = userAnswers
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

      val mockSessionRepository = mock[SessionRepository]
      val mockBarService = mock[BarsService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val barResponse = BarsVerificationResponse(
        accountNumberIsWellFormatted             = BarsResponse.Yes,
        sortCodeIsPresentOnEISCD                 = BarsResponse.Yes,
        sortCodeBankName                         = Some("Test Bank"),
        accountExists                            = BarsResponse.Yes,
        nameMatches                              = BarsResponse.Yes,
        sortCodeSupportsDirectDebit              = BarsResponse.Yes,
        sortCodeSupportsDirectCredit             = BarsResponse.Yes,
        nonStandardAccountDetailsRequiredForBacs = Some(BarsResponse.No),
        iban                                     = Some("GB29NWBK60161331926819"),
        accountName                              = Some("John Doe")
      )

      val bank = Bank(
        bankName       = "Test Bank",
        ddiVoucherFlag = Some("Y"),
        address = BankAddress(
          lines    = Seq("1 Bank Street"),
          town     = Some("London"),
          country  = Country("UK"),
          postCode = Some("EC1A 1AA")
        )
      )

      when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right((barResponse, bank))))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BarsService].toInstance(mockBarService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode"          -> "123212",
            "accountNumber"     -> "34211234",
            "auddisStatus"      -> "false"
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must handle BARService failure and call onFailedVerification when Left returned" in {
      val ua = userAnswers.setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

      val mockSessionRepository = mock[SessionRepository]
      val mockBarService = mock[BarsService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(models.errors.BarsErrors.BankAccountUnverified)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BarsService].toInstance(mockBarService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode"          -> "123212",
            "accountNumber"     -> "34211234",
            "auddisStatus"      -> "true"
          )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) must include("These account details could not be verified. Check your details or try different account details")
      }
    }

    "must log a warning when BARService returns Left" in {
      val ua = userAnswers
        .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

      val mockSessionRepository = mock[SessionRepository]
      val mockBarService = mock[BarsService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(models.errors.BarsErrors.BankAccountUnverified)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BarsService].toInstance(mockBarService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, yourBankDetailsRoute)
          .withFormUrlEncodedBody(
            "accountHolderName" -> "value 1",
            "sortCode"          -> "123212",
            "accountNumber"     -> "34211234",
            "auddisStatus"      -> "true"
          )

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }
    }

    "onFailedVerification" - {

      "NotLocked stays on page with errors" in new SetUp {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(models.errors.BarsErrors.BankAccountUnverified)))
        when(mockLockService.updateLockForUser(any())(any())).thenReturn(Future.successful(notLocked))

        val application: Application = applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BarsService].toInstance(mockBarService),
            bind[LockService].toInstance(mockLockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, yourBankDetailsRoute)
            .withFormUrlEncodedBody(
              "accountHolderName" -> "value 1",
              "sortCode"          -> "123212",
              "accountNumber"     -> "34211234",
              "auddisStatus"      -> "true"
            )

          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
          contentAsString(result) must include("These account details could not be verified")

          session(result).get(LockExpirySessionKey) mustBe None
        }
      }

      "locked + verified & flag=false, redirects to reach-limit" in new SetUp {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(models.errors.BarsErrors.NameMismatch)))
        when(mockLockService.updateLockForUser(any())(any())).thenReturn(Future.successful(lockedAndVerified))
        when(mockLockService.isUserLocked(any())(any())).thenReturn(Future.successful(lockedAndVerified))

        val application: Application = applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.secret.key" -> "aT3m3h0a2bS2S0Gv4bQ1aZ0cA4dF6gJ7mP8qR9tU2wX3yZ4C5dE6fG7hI8jK9LmN"
          )
          .overrides(
            bind[HttpFilters].to[NoHttpFilters],
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BarsService].toInstance(mockBarService),
            bind[LockService].toInstance(mockLockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, yourBankDetailsRoute)
            .withFormUrlEncodedBody(
              "accountHolderName" -> "value 1",
              "sortCode"          -> "123212",
              "accountNumber"     -> "34211234",
              "auddisStatus"      -> "true"
            )

          val result = route(application, request).value

          redirectLocation(result).value mustEqual routes.ReachedLimitController.onPageLoad().url
          session(result).get(LockExpirySessionKey) mustBe Some(expiryDateTime.toString)
        }
      }

      "locked + verified & flag=true marks unverified, redirects to account-details-not-verified" in new SetUp {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(models.errors.BarsErrors.BankAccountUnverified)))
        when(mockLockService.updateLockForUser(any())(any())).thenReturn(Future.successful(lockedAndVerified))
        when(mockLockService.markUserAsUnverifiable(any())(any())).thenReturn(Future.successful(lockedAndUnverified))

        val application: Application = applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.secret.key" -> "aT3m3h0a2bS2S0Gv4bQ1aZ0cA4dF6gJ7mP8qR9tU2wX3yZ4C5dE6fG7hI8jK9LmN"
          )
          .overrides(
            bind[HttpFilters].to[NoHttpFilters],
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BarsService].toInstance(mockBarService),
            bind[LockService].toInstance(mockLockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, yourBankDetailsRoute)
            .withFormUrlEncodedBody(
              "accountHolderName" -> "value 1",
              "sortCode"          -> "123212",
              "accountNumber"     -> "34211234",
              "auddisStatus"      -> "true"
            )

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.AccountDetailsNotVerifiedController.onPageLoad().url
          session(result).get(LockExpirySessionKey) mustBe Some(expiryDateTime.toString)
        }
      }

      "locked + unverified, uses status and redirects to account-details-not-verified" in new SetUp {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(models.errors.BarsErrors.NameMismatch)))
        when(mockLockService.updateLockForUser(any())(any())).thenReturn(Future.successful(lockedAndUnverified))
        when(mockLockService.isUserLocked(any())(any())).thenReturn(Future.successful(lockedAndUnverified))

        val application: Application = applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.secret.key" -> "aT3m3h0a2bS2S0Gv4bQ1aZ0cA4dF6gJ7mP8qR9tU2wX3yZ4C5dE6fG7hI8jK9LmN"
          )
          .overrides(
            bind[HttpFilters].to[NoHttpFilters],
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BarsService].toInstance(mockBarService),
            bind[LockService].toInstance(mockLockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, yourBankDetailsRoute)
            .withFormUrlEncodedBody(
              "accountHolderName" -> "value 1",
              "sortCode"          -> "123212",
              "accountNumber"     -> "34211234",
              "auddisStatus"      -> "true"
            )

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.AccountDetailsNotVerifiedController.onPageLoad().url
          session(result).get(LockExpirySessionKey) mustBe Some(expiryDateTime.toString)
        }
      }

      "mark returns 409 then falls back to status" in new SetUp {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockLockService.updateLockForUser(any())(any())).thenReturn(Future.successful(lockedAndVerified))
        when(mockBarService.barsVerification(any[String], any[YourBankDetails])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(models.errors.BarsErrors.BankAccountUnverified)))

        when(mockLockService.markUserAsUnverifiable(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("conflict", Error409, Error409)))

        when(mockLockService.isUserLocked(any())(any())).thenReturn(Future.successful(lockedAndUnverified))

        val application: Application = applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.secret.key" -> "aT3m3h0a2bS2S0Gv4bQ1aZ0cA4dF6gJ7mP8qR9tU2wX3yZ4C5dE6fG7hI8jK9LmN"
          )
          .overrides(
            bind[HttpFilters].to[NoHttpFilters],
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BarsService].toInstance(mockBarService),
            bind[LockService].toInstance(mockLockService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, yourBankDetailsRoute)
            .withFormUrlEncodedBody(
              "accountHolderName" -> "value 1",
              "sortCode"          -> "123212",
              "accountNumber"     -> "34211234",
              "auddisStatus"      -> "true"
            )

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.AccountDetailsNotVerifiedController.onPageLoad().url
          session(result).get(LockExpirySessionKey) mustBe Some(expiryDateTime.toString)
        }
      }
    }
  }

  trait SetUp {
    val ua: UserAnswers = userAnswers
      .setOrException(PersonalOrBusinessAccountPage, PersonalOrBusinessAccount.Personal)

    val Error409 = 409
    val expiryDateTime: Instant = Instant.parse("2025-09-03T12:00:00Z")

    val mockSessionRepository: SessionRepository = mock[SessionRepository]
    val mockBarService: BarsService = mock[BarsService]
    val mockLockService: LockService = mock[LockService]

    val notLocked: LockResponse =
      LockResponse("_", 0, isLocked = false, unverifiable = None, createdAt = None, lastUpdated = None, lockoutExpiryDateTime = None)

    val lockedAndVerified: LockResponse =
      LockResponse("_",
                   0,
                   isLocked              = true,
                   unverifiable          = Some(false),
                   createdAt             = None,
                   lastUpdated           = None,
                   lockoutExpiryDateTime = Some(expiryDateTime)
                  )

    val lockedAndUnverified: LockResponse =
      LockResponse("_",
                   0,
                   isLocked              = true,
                   unverifiable          = Some(true),
                   createdAt             = None,
                   lastUpdated           = None,
                   lockoutExpiryDateTime = Some(expiryDateTime)
                  )
  }
}
