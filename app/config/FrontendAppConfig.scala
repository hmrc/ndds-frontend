/*
 * Copyright 2026 HM Revenue & Customs
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

package config

import com.google.inject.{Inject, Singleton}
import models.DirectDebitSource
import models.DirectDebitSource.*
import play.api.{ConfigLoader, Configuration}
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")

  val paymentDelayDynamicAuddisEnabled: Int = configuration.get[Int]("working-days-delay.dynamic-delay-with-auddis-enabled")
  val paymentDelayDynamicAuddisNotEnabled: Int = configuration.get[Int]("working-days-delay.dynamic-delay-with-auddis-not-enabled")
  val TWO_WORKING_DAYS: Int = configuration.get[Int]("working-days-delay.two-days")
  val THREE_WORKING_DAYS: Int = configuration.get[Int]("working-days-delay.three-days")
  val TEN_WORKING_DAYS: Int = configuration.get[Int]("working-days-delay.ten-days")

  def PtaBtaUrl(directDebitSource: DirectDebitSource): String =
    println(directDebitSource.toString + " this is the source")
    if (directDebitSource.toString.equals("sa") || directDebitSource.toString.equals("tc")) {
      Some(configuration.get[String]("urls.ptaUrl"))
        .filter(_.nonEmpty)
        .getOrElse(testOnly.controllers.routes.DirectDebitConfirmationController.showPtaPage.url)
    } else {
      Some(configuration.get[String]("urls.btaUrl"))
        .filter(_.nonEmpty)
        .getOrElse(testOnly.controllers.routes.DirectDebitConfirmationController.showBtaPage.url)
    }

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String = configuration.get[String]("urls.signOut")
  val payingHmrcUrl: String = configuration.get[String]("urls.payingHmrc")
  val hmrcHelplineUrl: String = configuration.get[String]("urls.hmrcHelpline")
  val selfAssessmentUrl: String = configuration.get[String]("urls.selfAssessment")
  val paymentProblemUrl: String = configuration.get[String]("urls.paymentProblem")
  val hmrcOnlineServiceDeskUrl: String = configuration.get[String]("urls.hmrcOnlineServiceDesk")
  val govUKUrl: String = configuration.get[String]("urls.govUK")
  val paymentEarlyLateUrl: String = configuration.get[String]("urls.paymentEarlyLate")
  val p11DPaymentUrl: String = configuration.get[String]("urls.p11DPayment")
  val payMonthlyUrl: String = configuration.get[String]("urls.payMonthly")
  val payQuarterlyUrl: String = configuration.get[String]("urls.payQuarterly")

  private lazy val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/ndds-frontend"

  val languageTranslationEnabled: Boolean = configuration.get[Boolean]("features.welsh-translation")
  val maxNumberDDIsAllowed: Int = configuration.get[Int]("features.maxNumberDDIsAllowed")
  val maxNumberPPsAllowed: Int = configuration.get[Int]("features.maxNumberPPsAllowed")
  val isLockServiceEnabled: Boolean = configuration.get[Boolean]("features.enableLockService")
  val isSaBppEnabled: Boolean = configuration.get[Boolean]("features.sa-bpp")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val minimumLiabilityAmount: BigDecimal =
    BigDecimal(configuration.get[String]("payment-validation.minimumLiabilityAmount"))

  // Derived payment plan config
  val tcTotalNumberOfPayments: Int =
    configuration.get[Int]("paymentSchedule.tc.totalNumberOfPayments")

  val tcNumberOfEqualPayments: Int =
    configuration.get[Int]("paymentSchedule.tc.numberOfEqualPayments")

  val macKey: String =
    configuration.get[String]("mac.key")

  val bacsNumber: String =
    configuration.get[String]("barsClient.serviceUserNumber")

  val tcMonthsUntilSecondPayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilSecondPayment")

  val tcMonthsUntilPenultimatePayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilPenultimatePayment")

  val tcMonthsUntilFinalPayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilFinalPayment")

}
