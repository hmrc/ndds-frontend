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
import forms.AmendPlanEndDateFormProvider
import models.responses.{DirectDebitDetails, DuplicateCheckResponse, PaymentPlanDetails, PaymentPlanResponse}
import models.{NextPaymentValidationResult, NormalMode, PaymentPlanType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PaymentPlanDetailsQuery
import services.NationalDirectDebitService
import views.html.AmendPlanEndDateView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class AmendPlanEndDateControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()
  private val formProvider = new AmendPlanEndDateFormProvider()
  private def form = formProvider()
  val fixedToday: LocalDate = LocalDate.of(2025, 1, 1)

  lazy val routeGet: String = routes.AmendPlanEndDateController.onPageLoad(NormalMode).url
  lazy val routePost: String = routes.AmendPlanEndDateController.onSubmit(NormalMode).url
  lazy val amendAmountRoute: String = routes.AmendPaymentAmountController.onPageLoad(NormalMode).url
  lazy val confirmationRoute: String = routes.AmendPaymentPlanConfirmationController.onPageLoad(NormalMode).url

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routeGet)

  def postRequestWithDate(date: LocalDate): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, routePost)
      .withFormUrlEncodedBody(
        "value.day"   -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year"  -> date.getYear.toString
      )

  "AmendPlanEndDateController" - {
    val mockService = mock[NationalDirectDebitService]

    val directDebitDetails = DirectDebitDetails(
      bankSortCode       = Some("123456"),
      bankAccountNumber  = Some("12345678"),
      bankAccountName    = Some("UK Bank"),
      auDdisFlag         = true,
      submissionDateTime = LocalDateTime.now()
    )

    val planDetails = PaymentPlanDetails(
      hodService                = "SA",
      planType                  = PaymentPlanType.BudgetPaymentPlan.toString,
      paymentReference          = "987654321K",
      submissionDateTime        = LocalDateTime.now(),
      scheduledPaymentAmount    = Some(BigDecimal(1500)),
      scheduledPaymentStartDate = Some(fixedToday),
      scheduledPaymentEndDate   = Some(fixedToday.plusDays(10)),
      scheduledPaymentFrequency = Some("Monthly"),
      initialPaymentStartDate   = Some(fixedToday),
      initialPaymentAmount      = Some(BigDecimal(1500)),
      suspensionStartDate       = None,
      suspensionEndDate         = None,
      balancingPaymentAmount    = None,
      balancingPaymentDate      = None,
      totalLiability            = Some(BigDecimal(1500)),
      paymentPlanEditable       = true
    )

    val paymentPlanResponse = PaymentPlanResponse(directDebitDetails, planDetails)

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
        running(app) {
          val result = route(app, getRequest()).value
          val view = app.injector.instanceOf[AmendPlanEndDateView]
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, Call("GET", amendAmountRoute))(getRequest(), messages(app)).toString
        }
      }

      "must populate view when AmendPlanEndDatePage previously answered" in {
        val userAnswers = emptyUserAnswers.set(AmendPlanEndDatePage, fixedToday).success.value
        val app = applicationBuilder(Some(userAnswers)).build()
        running(app) {
          val result = route(app, getRequest()).value
          val view = app.injector.instanceOf[AmendPlanEndDateView]
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(Some(fixedToday)), NormalMode, Call("GET", amendAmountRoute))(getRequest(),
                                                                                                                         messages(app)
                                                                                                                        ).toString
        }
      }

      "must redirect to Journey Recovery when no data" in {
        val app = applicationBuilder(None).build()
        running(app) {
          val result = route(app, getRequest()).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must remove AmendPlanEndDatePage when date is cleared (None submitted)" in {
        val userAnswers = emptyUserAnswers.set(AmendPlanEndDatePage, fixedToday).success.value
        val app = applicationBuilder(Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(app) {
          val request = FakeRequest(POST, routePost)
          val result = route(app, request).value
          status(result) mustEqual SEE_OTHER
        }
      }

      "must show error when nextPaymentDateValid = false" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value

        val app = applicationBuilder(Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(app) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockService.calculateNextPaymentDate(any(), any(), any())(any))
            .thenReturn(Future.successful(NextPaymentValidationResult(None, nextPaymentDateValid = false)))

          val view = app.injector.instanceOf[AmendPlanEndDateView]
          val request = postRequestWithDate(fixedToday.plusDays(5))
          val result = route(app, request).value

          val errorForm = form.fill(Some(fixedToday.plusDays(5))).withError("value", "amendPlanEndDate.error.nextPaymentDateValid")

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(errorForm, NormalMode, Call("GET", amendAmountRoute))(request, messages(app)).toString
        }
      }

      "must redirect to DuplicateWarning page when duplicate plan detected" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1900))
          .success
          .value

        val app = applicationBuilder(Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(app) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(DuplicateCheckResponse(true)))

          val result = route(app, postRequestWithDate(fixedToday.plusDays(2))).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DuplicateWarningController.onPageLoad(NormalMode).url
        }
      }

      "must redirect to confirmation page when valid end date and not duplicate" in {
        val userAnswers = emptyUserAnswers
          .set(PaymentPlanDetailsQuery, paymentPlanResponse)
          .success
          .value
          .set(ManagePaymentPlanTypePage, PaymentPlanType.BudgetPaymentPlan.toString)
          .success
          .value
          .set(AmendPaymentAmountPage, BigDecimal(1500))
          .success
          .value

        val app = applicationBuilder(Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(app) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(true)
          when(mockService.calculateNextPaymentDate(any(), any(), any())(any))
            .thenReturn(Future.successful(NextPaymentValidationResult(Some(fixedToday.plusDays(3)), nextPaymentDateValid = true)))
          when(mockService.isDuplicatePaymentPlan(any())(any(), any()))
            .thenReturn(Future.successful(DuplicateCheckResponse(false)))

          val result = route(app, postRequestWithDate(fixedToday.plusDays(7))).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual confirmationRoute
        }
      }

      "must throw exception when amendPaymentPlanGuard = false" in {
        val userAnswers = emptyUserAnswers
          .set(ManagePaymentPlanTypePage, "Invalid Plan")
          .success
          .value

        val app = applicationBuilder(Some(userAnswers))
          .overrides(bind[NationalDirectDebitService].toInstance(mockService))
          .build()

        running(app) {
          when(mockService.amendPaymentPlanGuard(any())).thenReturn(false)
          val request = postRequestWithDate(fixedToday)
          val thrown = intercept[Exception](route(app, request).value.futureValue)
          thrown.getMessage must include("NDDS Payment Plan Guard: Cannot amend this plan type: Invalid Plan")
        }
      }
    }
  }
}
