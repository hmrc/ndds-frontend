package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class DirectDebitSourceSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "DirectDebitSource" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(DirectDebitSource.values.toSeq)

      forAll(gen) {
        directDebitSource =>

          JsString(directDebitSource.toString).validate[DirectDebitSource].asOpt.value mustEqual directDebitSource
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!DirectDebitSource.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[DirectDebitSource] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(DirectDebitSource.values.toSeq)

      forAll(gen) {
        directDebitSource =>

          Json.toJson(directDebitSource) mustEqual JsString(directDebitSource.toString)
      }
    }
  }
}
