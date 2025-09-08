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

package utils

object NDDSConstants {
  /** The prefix for all ndds constants to uniquely identify them in jsps. */
  val PREFIX = "NDDS_"
  /** The Model Name for Bank Details. */
  val BANK_DETAILS: String = PREFIX + "bankDetails"
  /** The Model Name for Bank Details. */
  val EARLIEST_BANK_COLLECTION_DATE: String = PREFIX + "collectionDate"
  /** The Model Name for Bank Details. */
  val BARS_BLOCK_UNLOCK_TIME: String = PREFIX + "currentRetryEndTime"
  /** Model return object identifier for Payment Plan details dto. */
  val PAYMENT_DTO = "paymentDto"
  /** Model return object identifier for Payment Plan details dto. */
  val DIRECT_DEBIT_REFERENCE = "ddiRef"
  /** Model return object identifier for Payment Plan dto. */
  val PAYMENT_PLAN_DTO = "paymentPlan"
  /** Model return object identifier for Direct Debit dto. */
  val DIRECT_DEBIT_DTO = "directDebit"
  /** Direct Debit instruction reference number index. */
  val FIRST_INDEX = 1
  /** Direct Debit instruction reference number index. */
  val DDI_REF_INDEX = 2
  /** Direct Debit instruction reference number index. */
  val URI_INSTRUCTION = "instruction"
  /** The Constant EQUALS_CHAR. */
  val EQUALS_CHAR = '='
  /** The Constant AMPERSAND_CHAR. */
  val AMPERSAND_CHAR = '&'
  /** The Constant QMARK_CHAR. */
  val QMARK_CHAR = '?'
  /** The Constant Forward Slash. */
  val FORWARD_SLASH = '/'
  /** Value true. */
  val TRUE_SIG = "true"
  /** Value false. */
  val FALSE_SIG = "false"
  /** Payment Plans. */
  val BUDGET_PAYMENT_PLAN = "budgetpaymentplan"
  /** Payment Plans. */
  val SINGLE_PAYMENT_PLAN = "singlepaymentplan"
  /** Key for the service node in the property cache payment plans */
  val PLAN_TYPE_KEY = "hmrc.portal.ndds.paymentPlans.DirectDebitServices.PaymentPlan"
  /** Seperator. */
  val SEPERATOR = "."
  /** ACCOUNTS OFFICE PROPERTY */
  val ACCOUNTS_OFFICE_PROPERTY = ".accountsOffice"
  /** ACCOUNTS OFFICE Content ID */
  val ACCOUNTS_OFFICE_ID = "accountsOfficeId"
  /** Request parameter for maximum number of Direct debit instructions reached flag. */
  val MAX_DDIS_REACHED_PARAM: String = PREFIX + "maxDirectDebitsFlag"
  /** Request parameter for maximum number of Direct debit instructions reached flag. */
  val MAX_PAYMENT_PLANS_REACHED_PARAM: String = PREFIX + "maxPaymentPlansFlag"
  /** Model name to indicate if a new direct debit is being setup */
  val NEW_DIRECT_DEBIT_FLAG_PARAM = "_isNewDirectDebitFlag"
  /** Model Key for the Your services page url. */
  val YOUR_SERVICES_URL_MODEL_KEY: String = PREFIX + "yourServicesUrl"
  /** Ndds Welcome url. */
  val NDDS_WELCOME_URL = "/welcome"
  /** Ndds Direct Debit Instructions Summary uri. */
  val DIRECT_DEBITS_INSTRUCTIONS_URI = "/instructions"
  /** Ndds Direct Debit Payment Plan Summary uri. */
  val PAYMENT_PLAN_SUMMARY_URI = "/paymentplans"
  /** Model Key for the Your services page url. */
  val YOUR_SERVICES_URL = "/home"
  /** Model Key for holding the url to go back to when clicking next on a set up or add plan confirmation page. */
  val CONFIRMATION_RETURN_URL_MODEL_KEY: String = PREFIX + "confirmationReturnUrl"
  /** Model Key for holding the Current As of Date for display on the submission confirmation pages. */
  val CURRENT_DATE: String = PREFIX + "currentDate"
  /** Navigation State parameter to indicate a user has requested back from the change suspension view. */
  val FROM_CHANGE_SUSPENSION_VIEW = "fromChangeSuspensionView"
  /** Model name to indicate if it is new payment plan or amendment of an existing payment plan */
  val AMENDMENT_FLAG = "amendmentFlag"
  /** Save button name. */
  val SAVE_BUTTON_NAME = "_commandSave"
  /** Ndds payment plan details uri */
  val PAY_PLAN_DETAILS_URI = "/instruction/[ddiRef]/[payPlanType]/[ppRef]/details"
  /** Referrer parameter set on a GET request initially by a referring service.
   * If present, this will then be added to the navigation state against this same parameter name.
   */
  val REFERRER_PARM_NAME = "referrer"
  /** Model key for the amend payment plan confirmation page has a Date of Issue against todays date. */
  val DATE_OF_ISSUE: String = PREFIX + "dateOfIssue"
  /** A constant which represents length for MGD registration number */
  val MGD_REF_LENGTH = 14
  /** ASCII value for M */
  val M_VALUE = 45
  /** Key for the service node in the property cache payment plans. */
  val SERVICE_KEY = "hmrc.portal.ndds.paymentPlans.DirectDebitServices.Service."
  /** Property cache payment plan node. */
  val PAYMENT_PLAN = ".PaymentPlans.PaymentPlan"
  /** Property cache payment ref help. */
  val PAYMENT_REF_HELP = ".paymentReferenceHelp"
  /** Property cache payment ref hint. */
  val PAYMENT_REF_HINT = ".paymentReferenceHint"
  /** Payment Plan name. */
  val PAYMENT_PLAN_NAME = ".planName"
  /** The duplicate attempt model key */
  val DUPLICATE_ATTEMPT_PARAM_KEY = "duplicateAttempt"
  /** URI for rendering view - Page Not Found */
  val PAGE_NOT_FOUND_VIEW_URI = "handleNotFound"
}

