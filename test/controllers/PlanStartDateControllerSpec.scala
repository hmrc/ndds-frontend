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

import java.time.{LocalDate, ZoneOffset}
import base.SpecBase
import forms.PlanStartDateFormProvider
import models.DirectDebitSource.MGD
import models.PaymentPlanType.VariablePaymentPlan
import models.responses.EarliestPaymentDate
import models.{DirectDebitSource, NormalMode, PaymentPlanType, PlanStartDateDetails, UserAnswers, YourBankDetailsWithAuddisStatus}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{DirectDebitSourcePage, PaymentPlanTypePage, PlanStartDatePage, YourBankDetailsPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.RDSDatacacheService
import views.html.PlanStartDateView

import scala.concurrent.Future

class PlanStartDateControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new PlanStartDateFormProvider()

  private def form = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.now(ZoneOffset.UTC)

  val mockService: RDSDatacacheService = mock[RDSDatacacheService]

  private val expectedEarliestPlanStartDate: EarliestPaymentDate = EarliestPaymentDate("2025-02-06")
  private val formattedDate = "06 Feb 2025"
  private val formattedDateNumeric = "06 02 2025"
  private val earliestPlanStartDate = "06-02-2025"
  private val testDirectDebitSource: DirectDebitSource = MGD
  private val testPaymentPlanType: PaymentPlanType = VariablePaymentPlan
  private val validPlanStartDateDetails: PlanStartDateDetails = PlanStartDateDetails(validAnswer, earliestPlanStartDate)
  lazy val planStartDateRoute: String = routes.PlanStartDateController.onPageLoad(NormalMode).url
  lazy val paymentReferenceRoute: String = routes.PaymentReferenceController.onPageLoad(NormalMode).url
  
  val expectedUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(DirectDebitSourcePage, testDirectDebitSource).success.value
    .set(PaymentPlanTypePage, testPaymentPlanType).success.value
    .set(YourBankDetailsPage, YourBankDetailsWithAuddisStatus("testName", "123456", "123456", true)).success.value

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, planStartDateRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, planStartDateRoute)
      .withFormUrlEncodedBody(
        "value.day" -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year" -> validAnswer.getYear.toString
      )

  "planStartDate Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
        .overrides(bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.successful(expectedEarliestPlanStartDate))

      running(application) {
        val result = route(application, getRequest()).value

        val view = application.injector.instanceOf[PlanStartDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, formattedDate, formattedDateNumeric, testDirectDebitSource, Call("GET", paymentReferenceRoute))(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = expectedUserAnswers.set(PlanStartDatePage, validPlanStartDateDetails).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(userAnswers))(any()))
        .thenReturn(Future.successful(expectedEarliestPlanStartDate))

      running(application) {
        val view = application.injector.instanceOf[PlanStartDateView]

        val result = route(application, getRequest()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, formattedDate, formattedDateNumeric, testDirectDebitSource, Call("GET", paymentReferenceRoute))(getRequest(), messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the earliest payment date cannot be obtained" in {

      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
        .overrides(
          bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.failed(new Exception("bang")))

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.successful(expectedEarliestPlanStartDate))

      val application =
        applicationBuilder(userAnswers = Some(expectedUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[RDSDatacacheService].toInstance(mockService)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
        .overrides(bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.successful(expectedEarliestPlanStartDate))

      val request =
        FakeRequest(POST, planStartDateRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PlanStartDateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, formattedDate, formattedDateNumeric, testDirectDebitSource, Call("GET", paymentReferenceRoute))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/direct-debits/there-is-a-problem"
      }
    }

    "must redirect to Journey Recovery for a POST if the earliest payment date cannot be obtained and the data is valid" in {

      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
        .overrides(
          bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.failed(new Exception("bang")))

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the earliest payment date cannot be obtained and the data is invalid" in {

      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
        .overrides(
          bind[RDSDatacacheService].toInstance(mockService))
        .build()

      when(mockService.getEarliestPlanStartDate(ArgumentMatchers.eq(expectedUserAnswers))(any()))
        .thenReturn(Future.failed(new Exception("bang")))

      val request =
        FakeRequest(POST, planStartDateRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      running(application) {
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
