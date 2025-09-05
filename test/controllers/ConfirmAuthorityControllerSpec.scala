
package controllers

import base.SpecBase
import forms.ConfirmAuthorityFormProvider
import models.ConfirmAuthority
import org.apache.pekko.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.Application
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route, running, status}
import views.html.ConfirmAuthorityView

class ConfirmAuthorityControllerSpec extends SpecBase {
  
  "ConfirmAuthorityController" - {
    
    "must return OK and the correct view for a GET" in new SetUp {
      
      val application: Application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(GET, confirmAuthorityRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val view = application.injector.instanceOf[views.html.ConfirmAuthorityView]
        
        contentAsString(result) mustEqual view(form, NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }
  }
  
  trait SetUp {
    def onwardRoute: Call = Call("GET", "/foo")

    lazy val confirmAuthorityRoute: String = routes.ConfirmAuthorityController.onPageLoad().url

    val formProvider = new ConfirmAuthorityFormProvider()
    val form: Form[ConfirmAuthority] = formProvider()

    lazy val backLinkRoute: Call = routes.SetupDirectDebitPaymentController.onPageLoad()
  }
}
