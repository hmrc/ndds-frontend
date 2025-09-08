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
import org.apache.commons.lang3.StringUtils
import play.api.data.Forms.char
import utils.NDDSConstants

import java.util.regex.Pattern

object ReferenceTypeValidator {
  trait Validator[A <: DirectDebitSource] {
    def validate(reference: String): Boolean
  }
  /** This constant represents UTR Regex Format. */
  private val UTR_FORMAT = Pattern.compile("\\d{10}K")
  /** This constant represents COTAX Regex Format. */
  private val COTAX_FORMAT = Pattern.compile("\\d{10}A001\\d{2}A")
  /** Pattern for Tax Credits payment reference */
  private val TC_FORMAT = Pattern.compile("[A-Z]{2}\\d{12}N[A-Z]")
  /** This constant represents VAT Regex Format. */
  private val VAT_REF_FORMAT = Pattern.compile("\\d{9}")
  /** Array where the expected checkChar is at the index given by mod result. */
//  private val charRemainderMap = Array('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'X', 'J', 'K', 'L', 'M', 'N', 'Y', 'P', 'Q', 'R', 'S', 'T', 'Z', 'V', 'W')
  private val charRemainderMap = "ABCDEFGHXJKLMNYPQRSTZVW".toCharArray
  /** This constant represents NIC Regex Format */
  private val NIC_FORMAT = Pattern.compile("\\d{17}[\\d{1}|X]")
  /** This constant represents SDLT Regex Format */
  private val SDLT_FORMAT = Pattern.compile("\\d{9}M[A-Z]")
  /** This constant represents PAYE BROCS Regex Format */
  private val PAYE_BROCS_FORMAT = Pattern.compile("\\d{3}P[A-Z]\\d{7}")
  /** This constant represents PAYE Single Digit (Bit) Regex Format */
  private val PAYE_BIT_FORMAT = Pattern.compile("\\d{1}")
  /** This constant represents PAYE reference starting with 961 */
  private val PAYE961_START_STRING = "961"
  /** This constant represents PAYE prefix characters array */
  private val PAYE_REF_PREFIX = Array("N", "P")
  /** This constant represents PAYE thirteen character string rule */
  private val PAYE_THIRTEEN_CHAR_STRING = "X"
  /** This constant represents PAYE Seven Digit (Bit) Regex Format */
  private val PAYE_BIT_SEVEN_FORMAT = Pattern.compile("\\d{7}")
  /** This constant represents PAYE Sixth Character when 13th is 'X' and first 3 are 961 */
  private val PAYE961_SIXTH_VALUE = 0
  /** This constant represents MGD reference number Regex Format */
  private val MGD_REF_FORMAT = Pattern.compile("X[A-Z]M0000\\d{7}")
  private val SAFE_14_REF_FORAMT = Pattern.compile("X[A-Z][A-Z0-9]\\d{11}")
  private val E_INT_VAL = 37
  private val C_INT_VAL = 35
  private val L_INT_VAL = 44
  /** This constant represents SAFE Char 3 character Regex Format */
  private val SAFE_REF_CHAR3_NUMERIC_FORMAT = Pattern.compile("\\d{1}")
  private val SAFE_15_REF_FORAMT = Pattern.compile("X[A-Z]\\d{13}")
  private val SAFE_ECL_REF_FORMAT = Pattern.compile("X[A-Z]ECL\\d{10}")

  given Validator[PAYE.type] with {
    def validate(reference: String): Boolean = {
//      var ref = reference
      if (reference.trim.isEmpty || reference.length < 13 || reference.length > 13) return false
      val ref = {
        val firstChar = reference.substring(0, 1)
        if (PAYE_REF_PREFIX.contains(firstChar)) reference.tail else reference
      }
      val brocsRefNo = ref.take(12)
      if (!PAYE_BROCS_FORMAT.matcher(brocsRefNo).matches) return false
      if (ref.length == 13) {
        val thirteenthChar = ref.charAt(12).toString
        val firstThree = ref.take(3)
        val sixthChar = ref.charAt(5).asDigit
        val sevenChars = ref.slice(5, 12)

        val startCharRuleValid = {
          if (firstThree == PAYE961_START_STRING) {
            if (!PAYE_BIT_FORMAT.matcher(thirteenthChar).matches && thirteenthChar != PAYE_THIRTEEN_CHAR_STRING) {
              false
            } else if (thirteenthChar == PAYE_THIRTEEN_CHAR_STRING && sixthChar != PAYE961_SIXTH_VALUE) {
              false
            } else if (thirteenthChar != PAYE_THIRTEEN_CHAR_STRING && !PAYE_BIT_SEVEN_FORMAT.matcher(sevenChars).matches) {
              false
            } else {
              true
            }
          } else {
            PAYE_BIT_FORMAT.matcher(thirteenthChar).matches
          }
        }

        if (!startCharRuleValid) return false
      }
      val validPayeRefFlag = payeModCheckResult(ref)
      //TODO yearMonth
//      if (validPayeRefFlag && StringUtils.isNotBlank(yearMonth)) if (!YYMM_FORMAT.matcher(yearMonth).matches) return false
//      else {
//        val month = yearMonth.substring(2, 4).toInt
//        if ((month < 1) || (month > 13)) return false
//      }
      validPayeRefFlag
    }
  }

  given Validator[MGD.type] with {
    def validate(ref: String): Boolean = {
      if (ref.length != NDDSConstants.MGD_REF_LENGTH) return false
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

  given Validator[SA.type] with { //existing
    def validate(ref: String): Boolean = {
      UTR_FORMAT.matcher(ref).matches && modulusU11(ref)
    }
  }

  given Validator[CT.type] with {
    def validate(reference: String): Boolean = {
      COTAX_FORMAT.matcher(reference).matches && modulusU11(reference)
    }
  }

  given Validator[SDLT.type] with {
    def validate(reference: String): Boolean = {
      if (!SDLT_FORMAT.matcher(reference).matches) return false
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
      val checkChar = reference.charAt(indexOfCheckChar)
      // range of protected chars
      val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
      // Mod Check
      val expectedCheckChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
      checkChar == expectedCheckChar
    }
  }

  given Validator[VAT.type] with {
    def validate(ref: String): Boolean = {
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
      val total1 = modCheckTotal(weightings, protectedChars)
      val result1 = modCheckForVat(modDivisor, total1)
      var mod97Flag = false
      if (result1 == checkDigitVal) mod97Flag = true
      var mod9755Flag = false
      val total2 = total1 + 55
      val result2 = modCheckForVat(modDivisor, total2)
      if (result2 == checkDigitVal) mod9755Flag = true
      if ((mod97Flag && !mod9755Flag) || (!mod97Flag && mod9755Flag)) true else false
    }
  }

  given Validator[NIC.type] with {
    def validate(ref: String): Boolean = {
      if (!NIC_FORMAT.matcher(ref).matches || (!(ref.substring(0, 2) == "60"))) return false
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
      isValidSafe14Ref(ref) || isValidSafe15RefECL(ref) || isValidSafe15Ref(ref)
    }
  }

  given Validator[TC.type] with {
    def validate(ref: String): Boolean = {
      val invalidChars = Array('D', 'F', 'I', 'O', 'Q', 'U', 'V')
      val invalidPrefix = Array("FY", "GB", "NK", "TN", "ZZ")
      val tcPayRef = ref.toUpperCase
      val prefix = tcPayRef.substring(0, 2)
      val chkChar = tcPayRef.charAt(tcPayRef.length - 1)
      if (!TC_FORMAT.matcher(tcPayRef).matches) return false
      else if (!StringUtils.containsNone(prefix, invalidChars: _*) || StringUtils.indexOfAny(prefix, invalidPrefix: _*) != -1) return false
      else if (!isValidDate(Integer.valueOf(tcPayRef.substring(8, 10)), Integer.valueOf(tcPayRef.substring(10, 12)), Integer.valueOf(tcPayRef.substring(12, 14)))) return false
      else if (taxCreditModCheck(tcPayRef) != chkChar) return false
      true
    }
  }

  private def payeModCheckResult(ref: String): Boolean = {
    val modDivisor = 23
    val indexOfCheckChar = 4
    val indexOfFirstProtectedChar = 0
    val indexOfLastProtectedChar = 11

    val weightings = Array(9, 10, 11, 12, 0, 8, 7, 6, 5, 4, 3, 2, 1)

    val checkChar = ref.charAt(indexOfCheckChar)
    val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar + 1)

    val sumOfWeightedValues = protectedChars.zipWithIndex.foldLeft(0) {
      case (sum, (char, idx)) =>
        if (idx == indexOfCheckChar) sum
        else if (idx == 3) sum + 48 * weightings(idx) // hardcoded for 'p'
        else sum + Character.digit(char, 10) * weightings(idx)
    }

    val thirteenthChar = ref.charAt(12)
    val finalSum = sumOfWeightedValues + (
      if (thirteenthChar == PAYE_THIRTEEN_CHAR_STRING.charAt(0)) 41
      else Character.digit(thirteenthChar, 10)
      )

    val modResult = finalSum % modDivisor
    val expectedChar = charRemainderMap(modResult)

    checkChar == expectedChar
  }

  private def mgdModCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String) = {
    // the numeric values of protected chars to multiply by weightings
    val numericValues = mgdParseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    val modResult = sumOfWeightedValues % modDivisor
    val expectedChar = remainderMap(modResult)
    expectedChar
  }

  private def mgdParseIntArray(allDigits: String) = {
    val ints = new Array[Int](allDigits.length)
    //Adding char 'M' value to array
    ints(0) = NDDSConstants.M_VALUE
    for (i <- 1 until ints.length) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  private[validation] def isValidSafe14Ref(ref: String): Boolean = {
    // Validate length of reference
    if (ref.length != 14) return false
    // Check if the reference matches the SAFE 14 format and char 3 is not 'M'
    if (!SAFE_14_REF_FORAMT.matcher(ref).matches || ref.charAt(2) == 'M') return false
    // Weightings for the modulus check
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
    // Protected characters to check the modulus against
    val protectedChars = ref.substring(2, 14)
    // Perform the modulus check
    val expectedChar = safe14ModCheck(23, weightings, charRemainderMap, protectedChars)
    // The check character should match the expected value
    ref.charAt(1) == expectedChar
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
    modCheck(modDivisor, weightings, charRemainderMap, eclParseIntArray(protectedChars)) == checkChar
  }

  private[validation] def isValidSafe15Ref(ref: String): Boolean = {
    if (ref.length != 15) return false
    if (!(SAFE_15_REF_FORAMT.matcher(ref).matches || SAFE_ECL_REF_FORMAT.matcher(ref).matches)) return false
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

  private def eclParseIntArray(allDigits: String): Array[Int] = {
    val ints = new Array[Int](allDigits.length)
    ints(0) = E_INT_VAL
    ints(1) = C_INT_VAL
    ints(2) = L_INT_VAL
    for (i <- 3 until ints.length) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  private[validation] def parseIntArray(allDigits: String) = {
    val ints = new Array[Int](allDigits.length)
    for (i <- ints.indices) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  private def modCheck(modDivisor: Int, weightings: Array[Int], protectedChars: String) = {
    // the numeric values of protected chars to multiply by weightings
    val numericValues = parseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    sumOfWeightedValues % modDivisor
  }

  private def modCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], numericValues: Array[Int]): Char = {
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    remainderMap(sumOfWeightedValues % modDivisor)
  }

  private def modulusU11(reference: String): Boolean = {
    val modDivisor = 11
    val indexOfCheckChar = 0
    val indexOfFirstProtectedChar = 1
    val indexOfLastProtectedChar = 9
    val weightings = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)
    val remainderMap = "21987654321".toCharArray
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar + 1)
    modCheck(modDivisor, weightings, remainderMap, parseIntArray(protectedChars)) == checkChar
  }

  private def modCheckTotal(weightings: Array[Int], protectedChars: String): Int = {
    val numericValues = parseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    sumOfWeightedValues
  }

  private def modCheckForVat(modDivisor: Int, sumOfWeightValues: Int): Int = {
    var sumOfWeightedValues = sumOfWeightValues
    while (sumOfWeightedValues > modDivisor) sumOfWeightedValues = sumOfWeightedValues - modDivisor
    if (sumOfWeightedValues <= modDivisor) sumOfWeightedValues = modDivisor - sumOfWeightedValues
    sumOfWeightedValues
  }

  private def isValidDate(day: Int, month: Int, year: Int): Boolean = {
    // Check for valid month and day ranges
    if (month < 1 || month > 12 || day < 1 || day > 31) return false
    // Handle months with 30 days
    if (month == 4 || month == 6 || month == 9 || month == 11)
      return day <= 30
    // Handle February and leap year
    if (month == 2) {
      val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
      return day <= 29 && (day != 29 || leap)
    }
    // If none of the conditions failed, the date is valid
    true
  }

  private def taxCreditModCheck(tcPayRef: String): Char = {
    val weighting = Array(256, 128, 64, 32, 16, 8, 4, 2)
    val checkDigits = "ABCDEFGHJKLMNPQRSTVWXYZ".toArray // Exclude 'I', 'O', 'U'
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    val total = (0 until 8).map { i =>
      val value = if (i < 2)
        (characters.indexOf(tcPayRef.charAt(i)) + 33) * weighting(i)
      else
        tcPayRef.charAt(i).asDigit * weighting(i)
      value
    }.sum

    checkDigits(total % 23)
  }

  private def safe14ModCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String) = {
    val numericValues = safe14ParseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    remainderMap(sumOfWeightedValues % modDivisor)
  }

  private def safe14ParseIntArray(protectedChars: String) = {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val ints = new Array[Int](protectedChars.length)
    val char1 = protectedChars.substring(0, 1)
    if (SAFE_REF_CHAR3_NUMERIC_FORMAT.matcher(char1).matches) ints(0) = char1.toInt
    else ints(0) = characters.indexOf(protectedChars.charAt(0)) + 33
    for (i <- 1 until ints.length) {
      ints(i) = protectedChars.substring(i, i + 1).toInt
    }
    ints
  }

  def validate[A <: DirectDebitSource](reference: String)(using v: Validator[DirectDebitSource]): Boolean = {
    v.validate(reference)
  }

}
