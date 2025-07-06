package pages

import models.AuthorisedAccountHolder
import play.api.libs.json.JsPath

case object AuthorisedAccountHolderPage extends QuestionPage[AuthorisedAccountHolder] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "authorisedAccountHolder"
}
