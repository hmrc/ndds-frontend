/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package validation

import models.*
import models.DirectDebitSource.*
import utils.ModUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Try, Success, Failure}
object ReferenceTypeValidator {

  trait Validator[A <: DirectDebitSource] {
    def validate(reference: String): Boolean
  }


  given Validator[PAYE.type] with {
    def validate(reference: String): Boolean = {
      val tempReference = (ref: String) => if (reference.length >= 13) Right(ref) else Left(s"Error: reference $ref length less than 13")
      val updatedTempRef = (tempRef: String) => {
        (if (Set('N', 'P').contains(tempRef(0))) Right(tempRef.drop(1)) else Right(tempRef)).flatMap {
          ref =>
            if (ref.length <= 13) {
              Right(ref)
            } else {
              Left(s"Error: Updated reference $ref is greater than 13")
            }
        }
      }
      val isUpdatedRefFormatValid = (ref: String) => {
        (if (ref.take(12).matches("\\d{3}P[A-Z]\\d{7}")) Right(ref) else Left(s"Error: $ref is in an invalid format")).flatMap {
          validRef =>
            if (validRef.length == 13) {
              validRef.take(3) match {
                case "961" if validRef(12) == 'X' && validRef(5) == '0' => Right(validRef)
                case "961" if validRef(12).toString.matches("\\d") =>
                  if (validRef.substring(5, 12).matches("\\d{7}")) Right(validRef) else Left(s"Error: Invalid Ref")
                case "961" => Left("Error: Invalid Ref")
                case _ =>
                  if (validRef(12).toString.matches("\\d")) {
                    Right(validRef)
                  } else {
                    Left(s"Error: $validRef is not a decimal")
                  }

              }
            } else {
              Right(validRef)
            }

        }
      }

      val result = for {
        tempRef <- tempReference(reference)
        updatedRef <- updatedTempRef(tempRef)
        _ <- isUpdatedRefFormatValid(updatedRef)
      } yield (updatedRef)

      result.fold(_ => false, ModUtils.payeModCheckResult)

    }
  }

  given Validator[MGD.type] with {
    def validate(reference: String): Boolean = {
      val refLengthCheck = (ref: String) => if (ref.length == 14) Right(ref) else Left("Error: Invalid Length")
      val isValidFormat = (ref: String) => if (ref.matches("X[A-Z]M0000\\d{7}")) Right(ref) else Left(s"Error: $ref Invalid format")

      val result = for {
        _ <- refLengthCheck(reference)
        _ <- isValidFormat(reference)
      } yield (reference)


      result.fold(_ => false, ref => ModUtils.mod23(ref))
    }
  }

  given Validator[SA.type] with {
    def validate(reference: String): Boolean = {
      val isValidFormat = (ref: String) => if (ref.matches("\\d{10}K")) Right(ref) else Left(s"Error: $ref Invalid format")

      isValidFormat(reference).fold(_ => false, ModUtils.modulusU11)
    }
  }

  given Validator[CT.type] with {
    def validate(reference: String): Boolean = {
       val isValidFormat = (ref: String) => if (ref.matches( "\\d{10}A001\\d{2}A")) Right(ref) else Left(s"Error: $ref Invalid format")
       isValidFormat(reference).fold(_ => false, ModUtils.modulusU11)

    }
  }

  given Validator[SDLT.type] with {
    def validate(reference: String): Boolean = {
      val isValidFormat = (ref: String) => if (ref.matches("\\d{9}M[A-Z]")) Right(ref) else Left(s"Error: $ref Invalid format")
      isValidFormat(reference).fold(_ => false, ModUtils.modSDLT)

    }
  }

  given Validator[VAT.type] with {
    def validate(reference: String): Boolean = {
      val refLengthCheck = (ref: String) => if (ref.length >= 9) Right(ref) else Left(s"Error: too many characters")
      val isValidFormat = (ref: String) => if (ref.matches("\\d{9}")) Right(ref) else Left(s"Error: $ref Invalid format")

      val result = for{
        _ <- refLengthCheck(reference)
        _ <- isValidFormat(reference)
      }yield reference

      result.fold(_ => false, ModUtils.mod97)

    }
  }

  given Validator[NIC.type] with {
    def validate(reference: String): Boolean = {
      val checkFirstTwoChars = (ref: String) => if (ref.take(2) == "60") Right(ref) else Left(s"Error: too many characters")
      val isValidFormat = (ref: String) => if (ref.matches("\\d{17}[\\d{1}|X]")) Right(ref) else Left(s"Error: $ref Invalid format")

      val result = for{
        _ <- checkFirstTwoChars(reference)
        _ <- isValidFormat(reference)
      }yield reference

      result.fold(_ => false, ModUtils.mod11)

    }
  }

  given Validator[OL.type] with {
    def validate(reference: String): Boolean = {

      val thirdCharacter = 2
      val length14AndFormatCheck = reference.length == 14 && reference.matches("X[A-Z][A-Z0-9]\\d{11}") && (reference(thirdCharacter) != 'M')
      val length15AndFormatCheck = reference.length == 15 && (reference.matches("X[A-Z]ECL\\d{10}") || reference.matches("X[A-Z]\\d{13}"))

      (length14AndFormatCheck, length15AndFormatCheck) match {
        case (true, _) => ModUtils.modSafe14(reference)
        case (_, true) => ModUtils.modSafe15(reference)
        case _ => false
      }

    }
  }

  given Validator[TC.type] with {
    def validate(reference: String): Boolean = {
      val invalidChars = Set('D', 'F', 'I', 'O', 'Q', 'U', 'V')
      val invalidCharacterPairs = Set("FY", "GB", "NK", "TN", "ZZ")
      val invalidFormat = (ref: String) => if (ref.matches("[A-Z]{2}\\d{12}N[A-Z]")) Right(ref) else Left("Error: invalid format")
      val checkFirstTwoChars = (ref: String) => {
        if (invalidChars.contains(ref(0)) || invalidChars.contains(ref(1)))
          Left("Error: First 2 values contain invalid characters")
        else
          Right(ref)
      }
      val checkCharacterPairs = (ref:String) => {
        if (invalidCharacterPairs.contains(ref.take(2)))
          Left("Error: Contains invalid pairs")
        else
          Right(ref)
      }

      val checkDateFormat = (ref:String) => {
        val formatter = DateTimeFormatter.ofPattern("ddMMyy")

        val extractDate = reference.substring(8,14)
        Try(LocalDate.parse(extractDate, formatter)) match{
          case Success(_) => Right(ref)
          case Failure(_) => Left("Error: Invalid date format")
        }
      }


      val result = for{
        _ <- invalidFormat(reference)
        _ <- checkFirstTwoChars(reference)
        _ <- checkCharacterPairs(reference)
        _ <-  checkDateFormat(reference)
      }yield()
      result.fold(_ => false, _ => ModUtils.modTCRef(reference))


    }
  }


  def validate[A <: DirectDebitSource](reference: String)(using v: Validator[DirectDebitSource]): Boolean = {
    v.validate(reference)
  }


}