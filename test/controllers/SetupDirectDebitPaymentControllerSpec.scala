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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import base.SpecBase
//import models.{RDSDatacacheResponse, RDSDirectDebitDetails, UserAnswers}
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.inject.bind
//import play.api.mvc.Call
//import play.api.test.FakeRequest
//import play.api.test.Helpers.*
//import services.RDSDatacacheService
//import views.html.SetupDirectDebitPaymentView
//
//import scala.concurrent.Future
//
//class SetupDirectDebitPaymentControllerSpec extends SpecBase with MockitoSugar {
//
//  val mockService: RDSDatacacheService = mock[RDSDatacacheService]
//  val sequence: Seq[RDSDirectDebitDetails] = Seq.empty
//  val cacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(3, sequence)
//  val expectedUserAnswers: UserAnswers = UserAnswers(userAnswersId)
////    .set(RDSDatacacheResponsePage, cacheResponse).success.value
//
//  "SetupDirectDebitPayment Controller" - {
//    lazy val yourDirectDebitInstructionsRoute: String = routes.YourDirectDebitInstructionsController.onPageLoad().url
//
//    "must return OK and the correct view for a GET with no back link (DDI = 0) without Back link" in {
//      val application = applicationBuilder(userAnswers = Some(expectedUserAnswers))
//        .overrides(bind[RDSDatacacheService].toInstance(mockService))
//        .build()
//
//      when(mockService.retrieveAllDirectDebits(ArgumentMatchers.eq(""))(any()))
//        .thenReturn(Future.successful(cacheResponse))
//
//      running(application) {
//        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)
//
//        val result = route(application, request).value
//
//        val view = application.injector.instanceOf[SetupDirectDebitPaymentView]
//
//        status(result) mustEqual OK
//        contentAsString(result) mustEqual view(3, Call(GET, yourDirectDebitInstructionsRoute))(request, messages(application)).toString
//        contentAsString(result) must not include ("Back")
//      }
//    }
//
//    "must return OK and the correct view for a GET if there is back link (DDI > 1) with Back link" in {
//      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
//
//      running(application) {
//        val request = FakeRequest(GET, routes.SetupDirectDebitPaymentController.onPageLoad().url)
//
//        val result = route(application, request).value
//
//        status(result) mustEqual OK
//
//        contentAsString(result) must include ("Back")
//
//        contentAsString(result) must include ("Setup a direct debit payment")
//        contentAsString(result) must include ("Please note")
//
//        contentAsString(result) must include ("Start now")
//      }
//
//    }
//  }
//}
