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
import models.{ConfirmAuthority, Mode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when, doNothing}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.ConfirmAuthorityPage
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, redirectLocation, route, running, status}
import play.twirl.api.Html
import repositories.SessionRepository
import views.html.ConfirmAuthorityView
import play.api.test.Helpers.writeableOf_AnyContentAsFormUrlEncoded
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import services.AuditService

import scala.concurrent.Future

class ConfirmAuthorityControllerSpec extends SpecBase {

  private val mode: Mode = NormalMode
  private val onwardRoute: Call = Call("GET", "/foo")
  val mockAuditService: AuditService = mock[AuditService]

  "ConfirmAuthorityController onPageLoad" - {

    "return OK and render empty form when no prior answer" in {
      val mockView = mock[ConfirmAuthorityView]
      when(mockView.apply(any(), any(), any())(any(), any()))
        .thenReturn(Html("ok"))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[ConfirmAuthorityView].toInstance(mockView),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

      running(application) {
        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.ConfirmAuthorityController.onPageLoad(mode).url)

        val result = route(application, request).value
        status(result) mustBe OK

        val formCaptor = ArgumentCaptor.forClass(classOf[play.api.data.Form[_]])
        val modeCaptor = ArgumentCaptor.forClass(classOf[Mode])
        val callCaptor = ArgumentCaptor.forClass(classOf[play.api.mvc.Call])

        verify(mockView).apply(formCaptor.capture(), modeCaptor.capture(), callCaptor.capture())(any(), any())
        formCaptor.getValue.hasErrors mustBe false
        formCaptor.getValue.value mustBe None
        modeCaptor.getValue mustBe mode
        callCaptor.getValue mustBe routes.BankDetailsCheckYourAnswerController.onPageLoad(mode)
        verify(mockAuditService).sendEvent(any())(any(), any(), any())
      }
    }

    "pre-populate the form when an answer exists" in {
      val mockView = mock[ConfirmAuthorityView]
      when(mockView.apply(any(), any(), any())(any(), any()))
        .thenReturn(Html("ok"))

      val ua: UserAnswers =
        emptyUserAnswers.set(ConfirmAuthorityPage, ConfirmAuthority.Yes).success.value

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[ConfirmAuthorityView].toInstance(mockView),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAuthorityController.onPageLoad(mode).url)
        val result = route(application, request).value

        status(result) mustBe OK

        val formCaptor = ArgumentCaptor.forClass(classOf[play.api.data.Form[_]])
        verify(mockView).apply(formCaptor.capture(), any(), any())(any(), any())
        formCaptor.getValue.value mustBe Some(ConfirmAuthority.Yes)
      }
    }
  }

  "ConfirmAuthorityController onSubmit" - {

    "return BadRequest when the form is invalid" in {
      val mockView = mock[ConfirmAuthorityView]
      when(mockView.apply(any(), any(), any())(any(), any()))
        .thenReturn(Html("bad"))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[ConfirmAuthorityView].toInstance(mockView),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ConfirmAuthorityController.onSubmit(mode).url)
            .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "save YES, persist to session, and redirect via Navigator" in {
      val mockSessionRepo = mock[SessionRepository]
      when(mockSessionRepo.set(any())).thenReturn(Future.successful(true))
      doNothing().when(mockAuditService).sendEvent(any())(any(), any(), any())

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepo),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ConfirmAuthorityController.onSubmit(mode).url)
            .withFormUrlEncodedBody("value" -> ConfirmAuthority.Yes.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        val savedCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepo).set(savedCaptor.capture())
        savedCaptor.getValue.get(ConfirmAuthorityPage) mustBe Some(ConfirmAuthority.Yes)
      }
    }

    "save NO, persist to session, and redirect via Navigator" in {
      val mockSessionRepo = mock[SessionRepository]
      when(mockSessionRepo.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepo),
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ConfirmAuthorityController.onSubmit(mode).url)
            .withFormUrlEncodedBody("value" -> ConfirmAuthority.No.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        val savedCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepo).set(savedCaptor.capture())
        savedCaptor.getValue.get(ConfirmAuthorityPage) mustBe Some(ConfirmAuthority.No)
      }
    }

    "redirect to Journey Recovery on POST when no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ConfirmAuthorityController.onSubmit(mode).url)
            .withFormUrlEncodedBody("value" -> ConfirmAuthority.Yes.toString)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
