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
import models.PaymentPlanType.AmendPaymentPlan
import models.requests.{AmendChrisSubmissionRequest, ChrisSubmissionRequest}
import models.{DirectDebitSource, Mode, PaymentPlanType, PlanStartDateDetails, UserAnswers}
import pages.{AmendPaymentPlanSourcePage, AmendPaymentPlanTypePage, AmendPlanEndDatePage, AmendPlanStartDatePage, BankDetailsAddressPage, BankDetailsBankNamePage, PaymentAmountPage, PaymentPlanReferenceNumberPage, PaymentReferencePage, PaymentsFrequencyPage, QuestionPage, RegularPaymentAmountPage, TotalAmountDuePage, YourBankDetailsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{DirectDebitReferenceQuery, PaymentReferenceQuery}
import repositories.SessionRepository
import services.NationalDirectDebitService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import views.html.AmendPaymentPlanConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AmendPaymentPlanConfirmationController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalAction,
                                                       requireData: DataRequiredAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       view: AmendPaymentPlanConfirmationView,
                                                       nddService: NationalDirectDebitService,
                                                       sessionRepository: SessionRepository,
                                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging{

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      val userAnswers = request.userAnswers

      val (rows, backLink) = userAnswers.get(AmendPaymentPlanTypePage) match {
        case Some(PaymentPlanType.BudgetPaymentPlan.toString) =>
          (Seq(
            AmendPaymentPlanTypeSummary.row(userAnswers),
            AmendPaymentPlanSourceSummary.row(userAnswers),
            PaymentsFrequencySummary.rowData(userAnswers),
            AmendPlanStartDateSummary.rowData(userAnswers),
            AmendPaymentAmountSummary.row(PaymentPlanType.BudgetPaymentPlan.toString, userAnswers),
            AmendPlanEndDateSummary.row(userAnswers),
          ).flatten, routes.AmendPlanEndDateController.onPageLoad(mode))
        case _ =>
          (Seq(
            AmendPaymentPlanTypeSummary.row(userAnswers),
            AmendPaymentPlanSourceSummary.row(userAnswers),
            PaymentsFrequencySummary.rowData(userAnswers),
            AmendPlanEndDateSummary.rowData(userAnswers),
            AmendPaymentAmountSummary.row(PaymentPlanType.SinglePaymentPlan.toString, userAnswers),
            AmendPlanStartDateSummary.row(userAnswers),
          ).flatten, routes.AmendPlanStartDateController.onPageLoad(mode))
      }
      for {
        bankDetailsWithAuddisStatus <- Future.fromTry(Try(userAnswers.get(YourBankDetailsPage).get))
        directDebitReference <- Future.fromTry(Try(userAnswers.get(DirectDebitReferenceQuery).get))
        paymentReference <- Future.fromTry(Try(userAnswers.get(PaymentReferenceQuery).get))
        planType <- Future.fromTry(Try(userAnswers.get(AmendPaymentPlanTypePage).get))
      } yield {
        Ok(view(mode, paymentReference, directDebitReference, bankDetailsWithAuddisStatus.sortCode,
          bankDetailsWithAuddisStatus.accountNumber,rows, backLink))
      }
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val ua = request.userAnswers

      ua.get(DirectDebitReferenceQuery) match {
        case Some(ddiReference) =>
          val chrisRequest = buildChrisSubmissionRequest(ua, ddiReference)

          println("****************************************" + chrisRequest)

          nddService.submitChrisData(chrisRequest).flatMap { success =>
            if (success) {
              logger.info(s"CHRIS submission successful for DDI Ref [$ddiReference]")
              Future.successful(Redirect(routes.AmendPaymentPlanUpdateController.onPageLoad()))
            } else {
              logger.error(s"CHRIS submission failed for DDI Ref [$ddiReference]")
              Future.successful(
                Redirect(routes.JourneyRecoveryController.onPageLoad())
                  .flashing("error" -> "There was a problem submitting your direct debit. Please try again later.")
              )
            }
          }

        case None =>
          logger.error("Missing DirectDebitReference in UserAnswers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }


  private def buildChrisSubmissionRequest(
                                           userAnswers: UserAnswers,
                                           ddiReference: String
                                         ): ChrisSubmissionRequest = {
    implicit val ua: UserAnswers = userAnswers
    val serviceTypeStr: String = ua.get(AmendPaymentPlanSourcePage).getOrElse("SA") // fallback if missing
    val serviceType: DirectDebitSource = DirectDebitSource.objectMap.getOrElse(serviceTypeStr, DirectDebitSource.SA)
    val planStartDateDetails: Option[PlanStartDateDetails] = ua.get(AmendPlanStartDatePage).map { date =>
      PlanStartDateDetails(
        enteredDate = date,
        earliestPlanStartDate = date.toString // you can adjust this if you have a different logic
      )
    }

    ChrisSubmissionRequest(
      serviceType = serviceType,
      paymentPlanType = PaymentPlanType.BudgetPaymentPlan,
      paymentFrequency = ua.get(PaymentsFrequencyPage),
      paymentPlanReferenceNumber = ua.get(PaymentPlanReferenceNumberPage),
      yourBankDetailsWithAuddisStatus = required(YourBankDetailsPage),
      planStartDate = planStartDateDetails,
      planEndDate = ua.get(AmendPlanEndDatePage),
      paymentDate = None,
      yearEndAndMonth = None,
      ddiReferenceNo = ddiReference,
      paymentReference = required(PaymentReferencePage),
      totalAmountDue = ua.get(TotalAmountDuePage),
      paymentAmount = ua.get(PaymentAmountPage),
      regularPaymentAmount = ua.get(RegularPaymentAmountPage),
      calculation = None,
      amendPlan = true
    )
  }

  private def required[A](page: QuestionPage[A])(implicit ua: UserAnswers, rds: play.api.libs.json.Reads[A]): A =
    ua.get(page).getOrElse(throw new Exception(s"Missing details: ${page.toString}"))


}