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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  lazy val host: String = configuration.get[String]("host")
  lazy val appName: String = configuration.get[String]("appName")

  private lazy val contactHost = configuration.get[String]("contact-frontend.host")
  private lazy val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")

  lazy val paymentDelayDynamicAuddisEnabled: Int = configuration.get[Int]("working-days-delay.dynamic-delay-with-auddis-enabled")
  lazy val paymentDelayDynamicAuddisNotEnabled: Int = configuration.get[Int]("working-days-delay.dynamic-delay-with-auddis-not-enabled")
  lazy val paymentDelayFixed: Int = configuration.get[Int]("working-days-delay.fixed-delay")
  lazy val variableMgdFixedDelay: Int = configuration.get[Int]("working-days-delay.variable-mgd-fixed-delay")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val signOutUrl: String = configuration.get[String]("urls.signOut")
  lazy val payingHmrcUrl: String = configuration.get[String]("urls.payingHmrc")
  lazy val hmrcHelplineUrl: String = configuration.get[String]("urls.hmrcHelpline")
  lazy val selfAssessmentUrl: String = configuration.get[String]("urls.selfAssessment")
  lazy val paymentProblemUrl: String = configuration.get[String]("urls.paymentProblem")
  lazy val hmrcOnlineServiceDeskUrl: String = configuration.get[String]("urls.hmrcOnlineServiceDesk")
  lazy val govUkNDDSGuidanceUrl: String = configuration.get[String]("urls.govUkCISGuidance")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  lazy val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/ndds-frontend"

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  lazy val maxNumberDDIsAllowed: Int =
    configuration.get[Int]("features.maxNumberDDIsAllowed")

  lazy val isLockServiceEnabled: Boolean =
    configuration.get[Boolean]("features.enableLockService")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  lazy val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  lazy val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  lazy val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  lazy val minimumLiabilityAmount: BigDecimal =
    BigDecimal(configuration.get[String]("payment-validation.minimumLiabilityAmount"))

  // Derived payment plan config
  lazy val tcTotalNumberOfPayments: Int =
    configuration.get[Int]("paymentSchedule.tc.totalNumberOfPayments")

  lazy val tcNumberOfEqualPayments: Int =
    configuration.get[Int]("paymentSchedule.tc.numberOfEqualPayments")

  lazy val macKey: String =
    configuration.get[String]("mac.key")

  lazy val bacsNumber: String =
    configuration.get[String]("barsClient.serviceUserNumber")

  lazy val tcMonthsUntilSecondPayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilSecondPayment")

  lazy val tcMonthsUntilPenultimatePayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilPenultimatePayment")

  lazy val tcMonthsUntilFinalPayment: Int =
    configuration.get[Int]("paymentSchedule.tc.monthsUntilFinalPayment")

}
