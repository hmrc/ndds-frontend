package pages

import models.PaymentPlanType
import play.api.libs.json.JsPath

case object PaymentPlanTypePage extends QuestionPage[PaymentPlanType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "paymentPlanType"
}
