package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class PaymentPlanTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PaymentPlanType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PaymentPlanType.values.toSeq)

      forAll(gen) {
        paymentPlanType =>

          JsString(paymentPlanType.toString).validate[PaymentPlanType].asOpt.value mustEqual paymentPlanType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PaymentPlanType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PaymentPlanType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PaymentPlanType.values.toSeq)

      forAll(gen) {
        paymentPlanType =>

          Json.toJson(paymentPlanType) mustEqual JsString(paymentPlanType.toString)
      }
    }
  }
}
