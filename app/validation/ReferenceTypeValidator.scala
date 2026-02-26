/*
 * Copyright 2026 HM Revenue & Customs
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
import utils.ModUtils.*

import scala.util.Right

object ReferenceTypeValidator {
  trait Validator[A <: DirectDebitSource] {
    def validate(reference: String): Boolean
  }

  given Validator[PAYE.type] with {
    def validate(ref: String): Boolean = {
      val reference = ref.toUpperCase
      val tempReference = (ref: String) => if (ref.length >= 13) Right(ref) else Left(s"Error: reference $ref length less than 13")
      val updatedTempRef = (tempRef: String) => {
        (if (Set('N', 'P').contains(tempRef(0))) Right(tempRef.drop(1)) else Right(tempRef)).flatMap { ref =>
          if (ref.length <= 13) {
            Right(ref)
          } else {
            Left(s"Error: Updated reference $ref is greater than 13")
          }
        }
      }
      val isUpdatedRefFormatValid = (ref: String) => {
        (if (ref.take(12).matches(PAYE_BROCS_FORAMT)) Right(ref) else Left(s"Error: $ref is in an invalid format")).flatMap { validRef =>
          if (validRef.length == 13) {
            validRef.take(3) match {
              case PAYE961_START_STRING if validRef(12).isDigit || validRef(12) == 'X' =>
                Right(validRef)
              case PAYE961_START_STRING if validRef(12) == 'X' && validRef(5) == '0' =>
                Right(validRef)
              case PAYE961_START_STRING if validRef(12) != 'X' && validRef.substring(5, 11).matches(PAYE_BIT_SEVEN_FORMAT) => Right(validRef)
              case _ =>
                if (!validRef.substring(0, 2).matches(PAYE961_START_STRING) && validRef(12).isDigit) {
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
        tempRef    <- tempReference(reference)
        updatedRef <- updatedTempRef(tempRef)
        _          <- isUpdatedRefFormatValid(updatedRef)
      } yield updatedRef
      result.fold(_ => false, payeModCheckResult)
    }
  }

  given Validator[MGD.type] with {
    def validate(reference: String): Boolean = {
      val ref = reference.toUpperCase
      if (ref.length != MGD_REF_LENGTH) return false

      /** Check against MGD Ref number regex format.
        */
      if (!MGD_REF_FORMAT.matcher(ref).matches) return false

      // the value to divide sumOfWeightedValues by in the modulus calculation
      val modDivisor = 23
      // the index of the check character in ref
      val indexOfCheckChar = 1
      // the index of the first character protected by the check character
      val indexOfFirstProtectedChar = 2
      // the index of the last character protected by the check character
      val indexOfLastProtectedChar = 14
      // the weightings to multiply equivalent indices in numericValues by
      val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
      // expected value in remainderMap at the index given by the calculation
      val checkChar = ref.charAt(indexOfCheckChar)
      // range of protected chars
      val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
      val expectedChar = mgdModCheck(modDivisor, weightings, charRemainderMap, protectedChars)

      expectedChar == checkChar
    }
  }

  given Validator[SA.type] with {
    def validate(ref: String): Boolean = {
      val reference = ref.toUpperCase
      UTR_FORMAT.matcher(reference).matches && modulusU11(reference)
    }
  }

  given Validator[CT.type] with {
    def validate(ref: String): Boolean = {
      val reference = ref.toUpperCase
      COTAX_FORMAT.matcher(reference).matches && modulusU11(reference)
    }
  }

  given Validator[SDLT.type] with {

    /** This method validates SDLT Payment Reference Number.
      * @param reference
      *   String SDLT Payment Reference Number
      * @return
      *   boolean
      */
    def validate(reference: String): Boolean = {
      val ref = reference.toUpperCase

      /** Check against SDLT regex format.
        */
      if (!SDLT_FORMAT.matcher(ref).matches) return false
      // the value to divide sumOfWeightedValues by in the modulus calculation
      val modDivisor = 23
      // the index of the check character in ref
      val indexOfCheckChar = 10
      // the index of the first character protected by the check character
      val indexOfFirstProtectedChar = 0
      // the index of the last character protected by the check character
      val indexOfLastProtectedChar = 9
      // the weightings to multiply equivalent indices in numericValues by
      val weightings = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)
      // expected value in remainderMap at the index given by the calculation
      val checkChar = ref.charAt(indexOfCheckChar)
      // range of protected chars
      val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
      // Mod Check
      val expectedCheckChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
      checkChar == expectedCheckChar
    }
  }

  given Validator[VAT.type] with {
    def validate(reference: String): Boolean = {
      val ref = reference.toUpperCase
      if (ref.length < 9 || !VAT_REF_FORMAT.matcher(ref).matches) return false

      // the value to divide sumOfWeightedValues by in the first modulus calculation
      val modDivisor = 97

      // the index of the first character protected by the check character
      val indexOfFirstProtectedChar = 0

      // the index of the last character protected by the check character
      val indexOfLastProtectedChar = 7

      // the weightings to multiply equivalent indices in numericValues by
      val weightings = Array(8, 7, 6, 5, 4, 3, 2)

      // Check Digit Value
      val checkDigitVal = Integer.valueOf(ref.substring(7, 9))

      // range of protected chars
      val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)

      val total1 = modCheckTotal(modDivisor, weightings, protectedChars)
      val result1 = modCheckForVat(modDivisor, total1)

      var mod97Flag = false
      if (result1 == checkDigitVal) mod97Flag = true

      var mod9755Flag = false
      val total2 = total1 + 55
      val result2 = modCheckForVat(modDivisor, total2)
      if (result2 == checkDigitVal) mod9755Flag = true

      (mod97Flag && !mod9755Flag) || (!mod97Flag && mod9755Flag)
    }
  }

  given Validator[NIC.type] with {
    def validate(reference: String): Boolean = {
      val ref = reference.toUpperCase

      /** First we check for NICDN Format and then we check whether first two digits in reference number are 60 */
      if (!NIC_FORMAT.matcher(ref).matches || (!ref.substring(0, 2).equals("60"))) return false

      // the value to divide sumOfWeightedValues by in the modulus calculation
      val modDivisor = 11

      // the index of the check character in ref
      val indexOfCheckChar = 17

      // the index of the first character protected by the check character
      val indexOfFirstProtectedChar = 0

      // the index of the last character protected by the check character
      val indexOfLastProtectedChar = 17

      // the weightings to multiply equivalent indices in numericValues by
      val weightings = Array(8, 4, 6, 3, 5, 2, 1, 9, 10, 7, 8, 4, 6, 3, 5, 2, 1)

      // expected value in remainderMap at the index given by the calculation
      val checkChar = ref.charAt(indexOfCheckChar)
      val checkCharVal = Character.digit(checkChar, 10)

      // range of protected chars
      val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)

      val remainder = modCheck(modDivisor, weightings, protectedChars)

      var calculatedCheckCharVal = -1
      if (remainder == 0) calculatedCheckCharVal = 0
      else if (remainder == 1) return checkChar == 'X'
      else calculatedCheckCharVal = modDivisor - remainder

      checkCharVal == calculatedCheckCharVal
    }
  }

  given Validator[OL.type] with {
    def validate(ref: String): Boolean = {
      val reference = ref.toUpperCase
      isValidSafe14Ref(reference) || isValidSafe15RefECL(reference) || isValidSafe15Ref(reference)
    }
  }

  /** This method validates SAFE Payment Reference Number having 14 digits length.
    *
    * @param ref
    *   String SAFE Payment Reference Number
    * @return
    *   boolean
    */
  private def isValidSafe14Ref(ref: String): Boolean = {
    if (ref.length != 14) return false

    /** EARS00018219138 - not correctly validating format of reference Check against SAFE 14 char Reference Number regex format. Additional check that
      * char 3 is not M as this is reserved for MGD references
      */
    val refCharNo3 = ref.substring(2, 3)
    if (!SAFE_14_REF_FORMAT.matcher(ref).matches) return false
    else if (SAFE_REF_CHAR3.equals(refCharNo3)) return false
    // the value to divide sumOfWeightedValues by in the modulus calculation
    val modDivisor = 23
    // the index of the check character in ref
    val indexOfCheckChar = 1
    // the index of the first character protected by the check character
    val indexOfFirstProtectedChar = 2
    // the index of the last character protected by the check character
    val indexOfLastProtectedChar = 14
    // the weightings to multiply equivalent indices in numericValues by
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
    // expected value in remainderMap at the index given by the calculation
    val checkChar = ref.charAt(indexOfCheckChar)
    // range of protected chars
    val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = safe14ModCheck(modDivisor, weightings, charRemainderMap, protectedChars)
    expectedChar == checkChar
  }

  /** This method does Modulus check for 14 character SAFE references.
    *
    * @param modDivisor
    *   int
    * @param weightings
    *   int[]
    * @param remainderMap
    *   char[]
    * @param protectedChars
    *   String
    * @return
    *   CheckDigit Character
    */
  private def safe14ModCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String) = {
    // the numeric values of protected chars to multiply by weightings
    val numericValues = safe14ParseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    val modResult = sumOfWeightedValues % modDivisor
    remainderMap(modResult)
  }

  /** Convert an alphanumeric String to an int array.
    *
    * @param protectedChars
    *   may include a leading Alpha character which needs to be covered to a numeric value.
    * @return
    *   the int[]
    */
  private def safe14ParseIntArray(protectedChars: String) = {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val ints = new Array[Int](protectedChars.length)
    val char1 = protectedChars.substring(0, 1)
    if (SAFE_REF_CHAR3_NUMERIC_FORMAT.matcher(char1).matches) ints(0) = char1.toInt
    else ints(0)                                                      = characters.indexOf(protectedChars.charAt(0)) + 33
    for (i <- 1 until ints.length) {
      ints(i) = protectedChars.substring(i, i + 1).toInt
    }
    ints
  }

  /** This method validates SAFE Payment Reference Number having 15 digits length.
    *
    * @param ref
    *   String SAFE Payment Reference Number
    * @return
    *   boolean
    */
  private def isValidSafe15Ref(ref: String): Boolean = {
    if (ref.length != 15) return false

    /** EARS00018219138 - previously allowing alpha's in chars 3 - 15. Check against SAFE 15 char Reference Number regex format.
      */
    if (!(SAFE_15_REF_FORMAT.matcher(ref).matches || SAFE_ECL_REF_FORMAT.matcher(ref).matches)) return false
    // the value to divide sumOfWeightedValues by in the modulus calculation
    val modDivisor = 23
    // the index of the check character in ref
    val indexOfCheckChar = 1
    // the index of the first character protected by the check character
    val indexOfFirstProtectedChar = 2
    // the index of the last character protected by the check character
    val indexOfLastProtectedChar = 15
    // the weightings to multiply equivalent indices in numericValues by
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)
    // expected value in remainderMap at the index given by the calculation
    val checkChar = ref.charAt(indexOfCheckChar)
    // range of protected chars
    val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    try {
      val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
      expectedChar == checkChar
    } catch {
      case numberFormatException: NumberFormatException =>
        false
    }
  }

  private def isValidSafe15RefECL(ref: String): Boolean = {
    // check max length 15 and matches regex pattern
    if (ref.length != 15 || !SAFE_ECL_REF_FORMAT.matcher(ref).matches) return false
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 15
    // the weightings to multiply equivalent indices in numericValues by
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)
    // expected value in remainderMap at the index given by the calculation
    val checkChar = ref.charAt(indexOfCheckChar)
    // range of protected chars
    val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, eclParseIntArray(protectedChars))
    expectedChar == checkChar
  }

  given Validator[TC.type] with {
    def validate(reference: String): Boolean = {
      val invalidChars = Set('D', 'F', 'I', 'O', 'Q', 'U', 'V')
      val invalidPrefix = Set("FY", "GB", "NK", "TN", "ZZ")
      val tcPayRef = reference.toUpperCase
      val prefix = tcPayRef.take(2)
      val chkChar = tcPayRef.last

      TC_FORMAT.matcher(tcPayRef).matches &&
      !prefix.exists(invalidChars.contains) &&
      !invalidPrefix.contains(prefix) &&
      isValidDate(
        tcPayRef.substring(8, 10).toInt,
        tcPayRef.substring(10, 12).toInt,
        tcPayRef.substring(12, 14).toInt
      ) && taxCreditModCheck(tcPayRef) == chkChar

    }
  }

  def validate[A <: DirectDebitSource](reference: String)(using v: Validator[A]): Boolean = {
    v.validate(reference)
  }

}
