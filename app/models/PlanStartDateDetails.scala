package models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class PlanStartDateDetails(enteredDate: LocalDate, earliestPlanStartSate: String)

object PlanStartDateDetails {
  implicit val format: OFormat[PlanStartDateDetails] = Json.format

  def toPaymentDatePageData(enteredDate: LocalDate, earliestPlanStartSate: String): PlanStartDateDetails = {
    PlanStartDateDetails(enteredDate, earliestPlanStartSate)
  }

  def toLocalDate(planStartDateDetails: PlanStartDateDetails): LocalDate = {
    planStartDateDetails.enteredDate
  }
}
