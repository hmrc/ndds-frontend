package models

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsError, JsString, Json}

class ConfirmAuthoritySpec extends SpecBase {
  
  "ConfirmAuthority" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(ConfirmAuthority.values)

      forAll(gen) {
        confirmAuthority =>

          JsString(confirmAuthority.toString).validate[ConfirmAuthority].asOpt.value mustEqual confirmAuthority
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!ConfirmAuthority.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[ConfirmAuthority] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PersonalOrBusinessAccount.values)

      forAll(gen) {
        confirmAuthority =>

          Json.toJson(confirmAuthority) mustEqual JsString(confirmAuthority.toString)
      }
    }
  }
}
