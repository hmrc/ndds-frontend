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
import forms.AmendConfirmRemovePlanEndDateFormProvider
import models.responses.{DirectDebitDetails, PaymentPlanDetails, PaymentPlanResponse}
import models.{NormalMode, PaymentPlanType, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalactic.Prettifier.default
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import repositories.SessionRepository
import utils.DateTimeFormats.formattedDateTimeShort
import views.html.AmendConfirmRemovePlanEndDateView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class AmendConfirmRemovePlanEndDateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/direct-debits/check-amendment-details")

  val formProvider = new AmendConfirmRemovePlanEndDateFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val amendConfirmRemovePlanEndDate: String = routes.AmendConfirmRemovePlanEndDateController.onPageLoad(NormalMode).url
  lazy val amendConfirmRemovePlanEndDatePost: String = routes.AmendConfirmRemovePlanEndDateController.onSubmit(NormalMode).url

  private val testPlanReference = "PP123456"
  private val testEndDate = LocalDate.now().plusMonths(3)
  private val planType = "04"

  private val paymentPlanDetails: PaymentPlanDetails = PaymentPlanDetails(
    hodService                = "HOD1",
    planType                  = planType,
    paymentReference          = testPlanReference,
    submissionDateTime        = LocalDateTime.now(),
    scheduledPaymentAmount    = Some(BigDecimal(100)),
    scheduledPaymentStartDate = Some(LocalDate.now()),
    initialPaymentStartDate   = Some(LocalDate.now()),
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
      directDebitDetails = DirectDebitDetails(None, None, None, true, LocalDateTime.now()),
      paymentPlanDetails = paymentPlanDetails
    )

  "ConfirmRemovePlanEndDate Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, amendConfirmRemovePlanEndDate)
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendConfirmRemovePlanEndDateView]

        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form,
            NormalMode,
            testPlanReference,
            expectedPlanEndDate,
            routes.AmendingPaymentPlanController.onPageLoad()
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
        .success
        .value
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value
        .set(AmendConfirmRemovePlanEndDatePage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {

        val request = FakeRequest(GET, amendConfirmRemovePlanEndDate)
        val view = application.injector.instanceOf[AmendConfirmRemovePlanEndDateView]
        val result = route(application, request).value

        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form.fill(true),
            NormalMode,
            testPlanReference,
            expectedPlanEndDate,
            routes.AmendingPaymentPlanController.onPageLoad()
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(POST, amendConfirmRemovePlanEndDatePost)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(AmendPlanEndDatePage, testEndDate)
        .success
        .value
        .set(PaymentPlanDetailsQuery, paymentPlanResponse)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(POST, amendConfirmRemovePlanEndDate)
          .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AmendConfirmRemovePlanEndDateView]
        val expectedPlanEndDate = formattedDateTimeShort(testEndDate.toString)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(
            boundForm,
            NormalMode,
            testPlanReference,
            expectedPlanEndDate,
            routes.AmendingPaymentPlanController.onPageLoad()
          )(
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to System Error for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, amendConfirmRemovePlanEndDate)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to System Error for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(POST, amendConfirmRemovePlanEndDatePost)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
