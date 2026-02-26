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

package utils

import java.util.regex.Pattern

object ModUtils {

  private val M_VALUE = 45
  val MGD_REF_LENGTH = 14
  val charRemainderMap: Array[Char] = Array(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'X', 'J', 'K', 'L', 'M', 'N', 'Y', 'P', 'Q', 'R', 'S', 'T', 'Z', 'V', 'W'
  )
  private val E_INT_VAL = 37
  private val C_INT_VAL = 35
  private val L_INT_VAL = 44

  /** This constant represents UTR Regex Format. */
  val UTR_FORMAT: Pattern = Pattern.compile("\\d{10}K")

  /** This constant represents COTAX Regex Format. */
  val COTAX_FORMAT: Pattern = Pattern.compile("\\d{10}A001\\d{2}A")

  /** This constant represents NINO Regex Format. */
  val NINO_FORMAT: Pattern = Pattern.compile("[a-zA-Z]{2}\\d{6}[a-zA-Z]{0,1}")

  /** Pattern for Tax Credits payment reference */
  val TC_FORMAT: Pattern = Pattern.compile("[A-Z]{2}\\d{12}N[A-Z]")

  /** This constant represents VAT Regex Format. */
  val VAT_REF_FORMAT: Pattern = Pattern.compile("\\d{9}")

  /** This constant represents NIC Regex Format */
  val NIC_FORMAT: Pattern = Pattern.compile("\\d{17}[\\d{1}|X]")

  /** This constant represents SDLT Regex Format */
  val SDLT_FORMAT: Pattern = Pattern.compile("\\d{9}M[A-Z]")

  /** This constant represents PAYE BROCS Regex Format */
  val PAYE_BROCS_FORAMT = "\\d{3}P[A-Z]\\d{7}"

  /** This constant represents PAYE Single Digit (Bit) Regex Format */
  val PAYE_BIT_FORMAT: Pattern = Pattern.compile("\\d{1}")

  /** This constant represents PAYE reference starting with 961 */
  val PAYE961_START_STRING = "961"

  /** This constant represents PAYE prefix characters array */
  val PAYE_REF_PREFIX: Array[String] = Array("N", "P")

  /** This constant represents PAYE Seven Digit (Bit) Regex Format */
  val PAYE_BIT_SEVEN_FORMAT = "\\d{7}"

  /** This constant represents PAYE Sixth Character when 13th is 'X' and first 3 are 961 */
  val PAYE961_SIXTH_VALUE = 0

  /** This constant represents PAYE optional last 4 digits */
  val YYMM_FORMAT: Pattern = Pattern.compile("\\d{4}")

  /** This constant represents MGD reference number Regex Format */
  val MGD_REF_FORMAT: Pattern = Pattern.compile("X[A-Z]M0000\\d{7}")

  /** This constant represents SAFE reference number 14 char Regex Format */
  val SAFE_14_REF_FORMAT: Pattern = Pattern.compile("X[A-Z][A-Z0-9]\\d{11}")

  /** This constant represents SAFE reference number 15 char Regex Format */
  val SAFE_15_REF_FORMAT: Pattern = Pattern.compile("X[A-Z]\\d{13}")
  val SAFE_ECL_REF_FORMAT: Pattern = Pattern.compile("X[A-Z]ECL\\d{10}")
  val SAFE_REF_CHAR3_NUMERIC_FORMAT: Pattern = Pattern.compile("\\d{1}")
  val SAFE_REF_CHAR3 = "M"

  def modTCRef(reference: String): Boolean = {
    val chkChar = reference.toUpperCase().charAt(reference.length() - 1);
    chkChar == taxCreditModCheck(reference)
  }

  def taxCreditModCheck(reference: String): Char = {
    val ninoLength = 8
    val weighting = Array(256, 128, 64, 32, 16, 8, 4, 2)
    val checkDigits = Array('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z')
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val modDivisor = 23

    val sum = (0 until ninoLength).foldLeft(0) { (total, i) =>
      val value =
        if (i < 2) {
          (chars.indexOf(reference.charAt(i)) + 33) * weighting(i)
        } else {
          reference.substring(i, i + 1).toInt * weighting(i)
        }
      total + value
    }
    val remainder = sum % modDivisor
    checkDigits(remainder)
  }

  def modSDLT(reference: String) = {
    val modDivisor = 23
    val indexOfCheckChar = 10
    val indexOfFirstProtectedChar = 0
    val indexOfLastProtectedChar = 9
    val weightings = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
    expectedChar == checkChar

  }

  def modulusU11(reference: String): Boolean = {
    assert(reference.length >= 10, "Payment reference must be at least 10 characters long")
    val modDivisor = 11
    val indexOfCheckChar = 0
    val indexOfFirstProtectedChar = 1
    val indexOfLastProtectedChar = 9
    val weightings = Array(6, 7, 8, 9, 10, 5, 4, 3, 2)
    val remainderMap = "21987654321".toCharArray
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar + 1)
    val expectedChar = modCheck(modDivisor, weightings, remainderMap, parseIntArray(protectedChars))
    expectedChar == checkChar
  }

  def mod11(reference: String): Boolean = {
    val modDivisor = 11
    val indexOfCheckChar = 17
    val indexOfFirstProtectedChar = 0
    val indexOfLastProtectedChar = 17

    val weightings = Array(8, 4, 6, 3, 5, 2, 1, 9, 10, 7, 8, 4, 6, 3, 5, 2, 1)
    val checkChar = reference.charAt(indexOfCheckChar)
    val checkCharVal = Character.digit(checkChar, 10)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val remainder = modCheck(modDivisor, weightings, protectedChars)

    remainder match {
      case 1 => checkChar == 'X'
      case 0 => checkCharVal == 0
      case _ => checkCharVal == (modDivisor - remainder)
    }
  }

  def payeModCheckResult(ref: String) = {
    val PAYE_THIRTEEN_CHAR_STRING = 'X'
    val modDivisor = 23
    val indexOfCheckChar = 4
    val indexOfFirstProtectedChar = 0
    val indexOfLastProtectedChar = 11
    val weightings = Array(9, 10, 11, 12, 0, 8, 7, 6, 5, 4, 3, 2, 1)
    val checkChar = ref.charAt(indexOfCheckChar)
    val protectedChars = ref.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar + 1)
    val chars = protectedChars.toCharArray

    val sum = chars.zipWithIndex.foldLeft(0) {
      case (acc, (value, index)) if index == 4 => acc
      case (acc, (value, index)) if index == 3 => acc + (48 * weightings(index))
      case (acc, (value, index)) => {
        val protChar = Character.digit(value, 10)
        acc + (protChar * weightings(index))
      }
    }
    val charAtPositionThirteen = ref.charAt(12)
    val updatedSum = if (charAtPositionThirteen == PAYE_THIRTEEN_CHAR_STRING) {
      sum + 41
    } else {
      sum + Character.digit(charAtPositionThirteen, 10)
    }
    val modResult = updatedSum % modDivisor
    checkChar == charRemainderMap(modResult)
  }

  def mod23(reference: String): Boolean = {
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 14
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = mgdModCheck(modDivisor, weightings, charRemainderMap, protectedChars)
    checkChar == expectedChar
  }

  /** modCheckForVat
    *
    * @param modDivisor
    *   int
    * @param sumOfWeightedValues
    *   int
    * @return
    *   int
    */
  def modCheckForVat(modDivisor: Int, sumOfWeightedValues: Int) = {
    var value = sumOfWeightedValues
    while (value > modDivisor) value = value - modDivisor
    if (value <= modDivisor) value   = modDivisor - value
    value
  }
  def modCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], numericValues: Array[Int]): Char = {
    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) { case (acc, (value, index)) =>
      acc + (value * weightings(index))
    }
    val modResult = sumOfWeightedValues % modDivisor
    remainderMap(modResult)
  }

  def modCheck(modDivisor: Int, weightings: Array[Int], protectedChars: String) = {
    val numericValues = parseIntArray(protectedChars)
    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) { case (acc, (value, index)) =>
      acc + (value * weightings(index))
    }
    sumOfWeightedValues % modDivisor
  }

  /** This method does Modulus check for VAT module
    *
    * @param modDivisor
    *   int
    * @param weightings
    *   int[]
    * @param protectedChars
    *   String
    * @return
    *   CheckDigit Character
    */
  def modCheckTotal(modDivisor: Int, weightings: Array[Int], protectedChars: String) = {
    // the numeric values of protected chars to multiply by weightings
    val numericValues = parseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    sumOfWeightedValues
  }

  /** Method checks whether a date is valid
    *
    * @param day
    *   \- date
    * @param month
    *   \- month
    * @param year
    *   \- year
    * @return
    *   boolean value indicating if a valid date
    */
  def isValidDate(day: Int, month: Int, year: Int): Boolean = {
    if (month < 1 || month > 12) {
      return false
    }
    if (day < 1 || day > 31) {
      return false
    }
    if ((month == 4 || month == 6 || month == 9 || month == 11) && day == 31) {
      return false
    }
    if (month == 2) {
      val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
      if (day > 29 || (day == 29 && !leap)) {
        return false
      }
    }
    true
  }

  /** This method does Modulus check.
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
  def mgdModCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String): Char = {
    // the numeric values of protected chars to multiply by weightings
    val numericValues = mgdParseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    val modResult = sumOfWeightedValues % modDivisor
    remainderMap(modResult)
  }

  /** Convert a numeric String to an int array.
    *
    * @param allDigits
    *   a non-null String where all chars are digits.
    * @return
    *   the int[]
    */
  private def mgdParseIntArray(allDigits: String) = {
    val ints = new Array[Int](allDigits.length)
    // Adding char 'M' value to array
    ints(0) = M_VALUE
    for (i <- 1 until ints.length) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  def parseIntArray(allDigits: String): Array[Int] = {
    val ints = new Array[Int](allDigits.length)
    for (i <- ints.indices) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  def eclParseIntArray(allDigits: String): Array[Int] = {
    val ints = new Array[Int](allDigits.length)
    ints(0) = E_INT_VAL
    ints(1) = C_INT_VAL
    ints(2) = L_INT_VAL
    for (i <- 3 until ints.length) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

}
