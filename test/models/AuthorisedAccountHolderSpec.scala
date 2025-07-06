package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class AuthorisedAccountHolderSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "AuthorisedAccountHolder" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(AuthorisedAccountHolder.values.toSeq)

      forAll(gen) {
        authorisedAccountHolder =>

          JsString(authorisedAccountHolder.toString).validate[AuthorisedAccountHolder].asOpt.value mustEqual authorisedAccountHolder
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!AuthorisedAccountHolder.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[AuthorisedAccountHolder] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(AuthorisedAccountHolder.values.toSeq)

      forAll(gen) {
        authorisedAccountHolder =>

          Json.toJson(authorisedAccountHolder) mustEqual JsString(authorisedAccountHolder.toString)
      }
    }
  }
}
