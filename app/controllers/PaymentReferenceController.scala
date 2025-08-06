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

import controllers.actions.*
import forms.PaymentReferenceFormProvider

import models.{DirectDebitSource, Mode}
import navigation.Navigator
import pages.{DirectDebitSourcePage, PaymentReferencePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import validation.ReferenceTypeValidatorMap
import views.html.PaymentReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentReferenceController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            sessionRepository: SessionRepository,
                                            navigator: Navigator,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: PaymentReferenceFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: PaymentReferenceView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val answers = request.userAnswers
      val selectedAnswers = answers.get(DirectDebitSourcePage)
      val dds = selectedAnswers.flatMap(dds => DirectDebitSource.objectMap.get(dds.toString))
      val result = dds.flatMap(v => ReferenceTypeValidatorMap.validatorType(v))

      result match {
        case None =>
          Redirect(navigator.nextPage(PaymentReferencePage, mode, answers))
        case Some(_) =>

          val form = formProvider(None)

          val preparedForm = answers.get(PaymentReferencePage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, selectedAnswers))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val answers = request.userAnswers
      val selectedAnswers = answers.get(DirectDebitSourcePage)
      val dds = selectedAnswers.flatMap(dds => DirectDebitSource.objectMap.get(dds.toString))
      val result = dds.flatMap(v => ReferenceTypeValidatorMap.validatorType(v))
      
      result match {
        case None =>
          Future.successful(Redirect(navigator.nextPage(PaymentReferencePage, mode, answers)))
        case Some(serviceType) =>

          val form = formProvider(Some(serviceType))
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, selectedAnswers))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentReferencePage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(PaymentReferencePage, mode, updatedAnswers))
          )
      }
  }
}
