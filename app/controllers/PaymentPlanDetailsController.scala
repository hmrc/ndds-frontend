

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PaymentPlanDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentPlanDetailsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              val controllerComponents: ControllerComponents,
                                              view: PaymentPlanDetailsView,
                                              appConfig: FrontendAppConfig,
                                              nddService: NationalDirectDebitService
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    Future.successful(Ok(view()))
  }
}
