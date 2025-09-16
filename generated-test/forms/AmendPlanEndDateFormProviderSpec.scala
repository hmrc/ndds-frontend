package forms

import java.time.{LocalDate, ZoneOffset}
import forms.behaviours.DateBehaviours
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

class AmendPlanEndDateFormProviderSpec extends DateBehaviours {

  private implicit val messages: Messages = stubMessages()
  private val checkDate = LocalDate.of(2024, 4, 6)
  private val form = new AmendPlanEndDateFormProvider()()

  "AmendPlanEndDateFormProvider" - {

    "must bind valid dates" in {
      val validDate = checkDate
      val result = form.bind(
        Map(
          "value.day" -> validDate.getDayOfMonth.toString,
          "value.month" -> validDate.getMonthValue.toString,
          "value.year" -> validDate.getYear.toString
        )
      )
      result.errors mustBe empty
      result.value.flatten mustEqual Some(validDate)
    }

    "must fail with required error if partially completed" in {
      val result = form.bind(
        Map(
          "value.day" -> "",
          "value.month" -> "4",
          "value.year" -> ""
        )
      )
      result.errors must contain(FormError("value", "planEndDate.error.incomplete", Seq("date.error.day", "date.error.year")))
    }
  }
}
