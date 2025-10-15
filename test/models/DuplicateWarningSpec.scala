package models

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsError, JsString, Json}

class DuplicateWarningSpec extends SpecBase {

  "DuplicateWarning" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(DuplicateWarning.values)

      forAll(gen) { duplicateWarning =>

        JsString(duplicateWarning.toString).validate[DuplicateWarning].asOpt.value mustEqual duplicateWarning
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!DuplicateWarning.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>

        JsString(invalidValue).validate[DuplicateWarning] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(DuplicateWarning.values)

      forAll(gen) { duplicateWarning =>

        Json.toJson(duplicateWarning) mustEqual JsString(duplicateWarning.toString)
      }
    }
  }
}
