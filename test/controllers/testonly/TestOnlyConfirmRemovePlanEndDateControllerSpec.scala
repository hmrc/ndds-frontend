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

package controllers.testonly

import base.SpecBase
import forms.ConfirmRemovePlanEndDateFormProvider
import models.{NormalMode, PaymentPlanType, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import java.time.{LocalDate, LocalDateTime}
import utils.DateTimeFormats.formattedDateTimeShort
import views.html.testonly.TestOnlyConfirmRemovePlanEndDateView
import controllers.testonly.routes as testonlyRoutes
import scala.concurrent.Future

class TestOnlyConfirmRemovePlanEndDateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  private val formProvider = new ConfirmRemovePlanEndDateFormProvider()
  private val form = formProvider()

  private lazy val testOnlyConfirmRemovePlanEndDateRoute =
    testonlyRoutes.TestOnlyConfirmRemovePlanEndDateController
      .onPageLoad(NormalMode)
      .url

  // SIMAPLA plan reference is carried in PaymentPlanDetails.paymentReference
  private val simaplaPlanReference = "PP123456"

  // Use a fixed end date for determinism in tests
  private val testEndDate = LocalDate.of(2025, 12, 31)

  // Keep a consistent plan type value (string as stored in UA)
  private val planType = PaymentPlanType.BudgetPaymentPlan.toString

  private val paymentPlanDetails: PaymentPlanDetails = PaymentPlanDetails(
    hodService                = "HOD1",
    planType                  = planType,
    paymentReference          = simaplaPlanReference, // <- SIMAPLA ref comes from details
    submissionDateTime        = LocalDateTime.of(2025, 11, 29, 12, 0),
    scheduledPaymentAmount    = Some(BigDecimal(100)),
    scheduledPaymentStartDate = Some(LocalDate.of(2025, 11, 1)),
    initialPaymentStartDate   = Some(LocalDate.of(2025, 11, 1)),
    initialPaymentAmount      = Some(BigDecimal(50)),
    scheduledPaymentEndDate   = Some(testEndDate),
    scheduledPaymentFrequency = Some("Monthly"),
    suspensionStartDate       = None,
    suspensionEndDate         = None,
    balancingPaymentAmount    = None,
    balancingPaymentDate      = None,
    totalLiability            = Some(BigDecimal(600)),
    paymentPlanEditable       = true
  )

  private val paymentPlanResponse: PaymentPlanResponse =
    PaymentPlanResponse(
      directDebitDetails = DirectDebitDetails(None, None, None, auDdisFlag = true, submissionDateTime = LocalDateTime.of(2025, 11, 29, 12, 0)),
      paymentPlanDetails = paymentPlanDetails
    )

  "ConfirmRemovePlanEndDate Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        // Controller reads the end date from AmendPlanEndDatePage
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        val request = FakeRequest(GET, testOnlyConfirmRemovePlanEndDateRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[TestOnlyConfirmRemovePlanEndDateView]

        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form,
            NormalMode,
            simaplaPlanReference, // <- SIMAPLA ref from payment details
            expectedPlanEndDate, // <- formatted from AmendPlanEndDatePage
            testonlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad() // <- matches controller's back-link
          )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value
        .set(ConfirmRemovePlanEndDatePage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        val request = FakeRequest(GET, testOnlyConfirmRemovePlanEndDateRoute)
        val view = application.injector.instanceOf[TestOnlyConfirmRemovePlanEndDateView]
        val result = route(application, request).value

        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form.fill(true),
            NormalMode,
            simaplaPlanReference,
            expectedPlanEndDate,
            testonlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, testOnlyConfirmRemovePlanEndDateRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        val request = FakeRequest(POST, testOnlyConfirmRemovePlanEndDateRoute)
          .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[TestOnlyConfirmRemovePlanEndDateView]
        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(
            boundForm,
            NormalMode,
            simaplaPlanReference,
            expectedPlanEndDate,
            testonlyRoutes.TestOnlyAmendingPaymentPlanController.onPageLoad()
          )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        val request = FakeRequest(GET, testOnlyConfirmRemovePlanEndDateRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
        .build()

      running(application) {
        val request = FakeRequest(POST, testOnlyConfirmRemovePlanEndDateRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
