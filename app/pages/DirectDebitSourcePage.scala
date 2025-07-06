package pages

import models.DirectDebitSource
import play.api.libs.json.JsPath

case object DirectDebitSourcePage extends QuestionPage[DirectDebitSource] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "directDebitSource"
}
