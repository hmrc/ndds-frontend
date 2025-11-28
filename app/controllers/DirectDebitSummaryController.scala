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
import models.{NormalMode, PaymentPlanType, UserAnswers}
import models.responses.NddPaymentPlan
import pages.{AmendPaymentAmountPage, AmendPlanEndDatePage, AmendPlanStartDatePage, ManagePaymentPlanTypePage, SuspensionPeriodRangeDatePage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{NationalDirectDebitService, PaginationService}
import queries.{DirectDebitReferenceQuery, ExistingDirectDebitIdentifierQuery, PaymentPlanDetailsQuery, PaymentPlanReferenceQuery, PaymentPlansCountQuery}
import repositories.{DirectDebitCacheRepository, SessionRepository}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Card, CardTitle, SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AmendPaymentAmountSummary, AmendPaymentPlanSourceSummary, AmendPaymentPlanTypeSummary, DateSetupSummary}
import views.html.DirectDebitSummaryView
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.Utils.cleanConfirmationFlags

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class DirectDebitSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  val directDebitCache: DirectDebitCacheRepository,
  view: DirectDebitSummaryView,
  nddService: NationalDirectDebitService,
  sessionRepository: SessionRepository,
  paginationService: PaginationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val userAnswers = request.userAnswers
    val currentPage = request.getQueryString("page").flatMap(_.toIntOption).getOrElse(1)
    userAnswers.get(DirectDebitReferenceQuery) match {
      case Some(reference) =>
        cleanConfirmationFlags(userAnswers).flatMap { cleansedAnswers =>
          cleanseUserData(cleansedAnswers).flatMap { furtherCleansedAnswers =>
            nddService.retrieveDirectDebitPaymentPlans(request.userId, reference).flatMap { ddPaymentPlans =>
              for {
                updatedAnswers <- Future.fromTry(furtherCleansedAnswers.set(PaymentPlansCountQuery, ddPaymentPlans.paymentPlanCount))
                updatedAnswers <- Future.fromTry(updatedAnswers.set(DirectDebitReferenceQuery, reference))
                _              <- sessionRepository.set(updatedAnswers)
              } yield {

                val (title, heading) = if (ddPaymentPlans.paymentPlanList.nonEmpty) {
                  ("directDebitPaymentSummary.title.withPlans", "directDebitPaymentSummary.heading.withPlans")
                } else {
                  ("directDebitPaymentSummary.title.noPlans", "directDebitPaymentSummary.title.noPlans")
                }

                val paginationResult = paginationService.paginatePaymentPlans(
                  allPaymentPlans = ddPaymentPlans.paymentPlanList,
                  currentPage     = currentPage,
                  baseUrl         = routes.DirectDebitSummaryController.onPageLoad().url
                )
                Ok(
                  view(
                    reference,
                    ddPaymentPlans,
                    buildCards(paginationResult.paginatedData),
                    paginationViewModel = paginationResult.paginationViewModel,
                    title,
                    heading
                  )
                )
              }
            }
          }
        }
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onRedirect(directDebitReference: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updatedAnswers <- Future.fromTry(userAnswers.set(DirectDebitReferenceQuery, directDebitReference))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.DirectDebitSummaryController.onPageLoad())
  }

  def onRedirectToDirectDebitSource(directDebitReference: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val userId = request.userId
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(userId))
    for {
      directDebit    <- directDebitCache.getDirectDebit(directDebitReference)(userId)
      updatedAnswers <- Future.fromTry(userAnswers.set(ExistingDirectDebitIdentifierQuery, directDebit))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(routes.DirectDebitSourceController.onPageLoad(NormalMode))
  }

  private def buildCards(paymentPlanList: Seq[NddPaymentPlan])(implicit messages: Messages): Seq[SummaryList] = {
    paymentPlanList.map { plan =>
      SummaryList(
        card = Some(
          Card(
            title = Some(
              CardTitle(
                content = Text(messages("directDebitPaymentSummary.activePayment.summary.title", plan.paymentReference))
              )
            ),
            actions = Some(
              Actions(
                items = Seq(
                  ActionItem(
                    href               = routes.PaymentPlanDetailsController.onRedirect(plan.planRefNumber).url,
                    content            = Text(messages("directDebitPaymentSummary.activePayment.summary.action")),
                    visuallyHiddenText = Some(messages(plan.paymentReference))
                  )
                )
              )
            )
          )
        ),
        rows = buildSummaryRows(Seq(plan))
      )
    }
  }

  // this is to build rows based on the plan type - similar pp1
  private def buildSummaryRows(paymentPlanList: Seq[NddPaymentPlan])(implicit messages: Messages): Seq[SummaryListRow] = {
    paymentPlanList.flatMap { plan =>

      def optionalRow[T](maybeValue: Option[T])(build: T => SummaryListRow): Option[SummaryListRow] =
        maybeValue.map(build)

      plan.planType match {

        case PaymentPlanType.VariablePaymentPlan.toString =>
          Seq(
            optionalRow(Option(plan.planType))(v => AmendPaymentPlanTypeSummary.row(v)),
            optionalRow(Option(plan.hodService))(v => AmendPaymentPlanSourceSummary.row(v)),
            optionalRow(Option(plan.submissionDateTime))(v => DateSetupSummary.row(v))
          ).flatten

        case _ => // For Single, TaxCreditRepaymentPlan and Budget plan
          Seq(
            optionalRow(Option(plan.planType))(v => AmendPaymentPlanTypeSummary.row(v)),
            optionalRow(Option(plan.hodService))(v => AmendPaymentPlanSourceSummary.row(v)),
            optionalRow(Option(plan.submissionDateTime))(v => DateSetupSummary.row(v)),
            optionalRow(Option(plan.scheduledPaymentAmount))(amount => AmendPaymentAmountSummary.row(plan.planType, amount))
          ).flatten
      }
    }
  }

  private def cleanseUserData(userAnswers: UserAnswers): Future[UserAnswers] =
    for {
      updatedUserAnswers <- Future.fromTry(userAnswers.remove(PaymentPlanReferenceQuery))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(PaymentPlanDetailsQuery))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(ManagePaymentPlanTypePage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(AmendPaymentAmountPage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(AmendPlanStartDatePage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(AmendPlanEndDatePage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(SuspensionPeriodRangeDatePage))
      updatedUserAnswers <- Future.fromTry(updatedUserAnswers.remove(ExistingDirectDebitIdentifierQuery))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers
}
