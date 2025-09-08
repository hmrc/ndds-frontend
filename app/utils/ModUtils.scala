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

package utils

import java.util.regex.Pattern

object ModUtils {

  val nddsMValue = 45
//  val charRemainderMap = Array(
//    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
//    'X', 'J', 'K', 'L', 'M', 'N', 'Y', 'P',
//    'Q', 'R', 'S', 'T', 'Z', 'V', 'W'
//  )
  val charRemainderMap = "ABCDEFGHXJKLMNYPQRSTZVW".toCharArray

  private val E_INT_VAL = 37
  private val C_INT_VAL = 35
  private val L_INT_VAL = 44
  val SAFE_14_REF_FORMAT = Pattern.compile("X[A-Z][A-Z0-9]\\d{11}")
  val SAFE_15_REF_FORMAT = Pattern.compile("X[A-Z]\\d{13}")
  val SAFE_ECL_REF_FORMAT = Pattern.compile("X[A-Z]ECL\\d{10}")

  def modTCRef(reference: String) = {
    val chkChar = reference.toUpperCase().charAt(reference.length() - 1);

    chkChar == taxCreditModCheck(reference)
  }

  def isValidDate(day: Int, month: Int, year: Int): Boolean = {
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

  def taxCreditModCheck(tcPayRef: String): Char = {
    val weighting = Array(256, 128, 64, 32, 16, 8, 4, 2)
    val checkDigits = "ABCDEFGHJKLMNPQRSTVWXYZ".toArray  // Exclude 'I'
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

  def modSafe14(reference: String) = {
    println(s"**** in modSafe14 Reference: $reference")
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 14

    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
    val checkChar = reference.charAt(indexOfCheckChar)
    println(s"****** checkChar: $checkChar")

    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = safe14ModCheck(modDivisor, weightings, charRemainderMap, protectedChars)
    println(s"****** expectedChar: $expectedChar")
    checkChar == expectedChar
  }

  def modSafe15(reference: String) = {
    println(s"**** in modSafe15 Reference: $reference")
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 15

    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)
    val checkChar = reference.charAt(indexOfCheckChar)
    println(s"****** checkChar: $checkChar")
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    println(s"****** protectedChars: $protectedChars")
    try {
      val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
      println(s"****** expectedChar: $expectedChar")
      checkChar == expectedChar
    } catch {
      case numberFormatException: NumberFormatException =>
        false
    }
  }

  def modSafe15ECL(reference: String) = {
    println(s"**** in modSafe15ECL Reference: $reference")
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 15

    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)
    val checkChar = reference.charAt(indexOfCheckChar)
    println(s"****** checkChar: $checkChar")
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    println(s"****** protectedChars: $protectedChars")
    val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, eclParseIntArray(protectedChars))
    println(s"****** expectedChar: $expectedChar")

    checkChar == expectedChar
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

  def mod97(reference: String): Boolean = {
    val modDivisor = 97
    val indexOfFirstProtectedChar = 0

    val indexOfLastProtectedChar = 7

    val weightings = Array(8, 7, 6, 5, 4, 3, 2)

    val checkDigitVal = Integer.valueOf(reference.substring(7, 9))

    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)

    val total1 = modCheckTotal(weightings, protectedChars)
    val result1 = modCheckForVat(modDivisor, total1)
    val mod97Flag = result1 == checkDigitVal

    val total2 = total1 + 55;
    val result2 = modCheckForVat(modDivisor, total2)
    val mod9755Flag = result2 == checkDigitVal

    (mod97Flag, mod9755Flag) match {
      case (true, false) => true
      case (false, true) => true
      case _ => false
    }

  }

  def modCheckForVat(modDivisor: Int, sumOfWeightValues: Int): Int = {
    var sumOfWeightedValues = sumOfWeightValues
    while (sumOfWeightedValues > modDivisor) sumOfWeightedValues =  sumOfWeightedValues - modDivisor
    if (sumOfWeightedValues <= modDivisor) sumOfWeightedValues = modDivisor - sumOfWeightedValues
    sumOfWeightedValues
  }

  def modCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], numericValues: Array[Int]): Char = {
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    remainderMap(sumOfWeightedValues % modDivisor)
  }

  private def modCheck(modDivisor: Int, weightings: Array[Int], protectedChars: String) = {
    val numericValues = parseIntArray(protectedChars)

    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) {
      case (acc, (value, index)) => acc + (value * weightings(index))
    }

    sumOfWeightedValues % modDivisor
  }

  def modCheckTotal(weightings: Array[Int], protectedChars: String): Int = {
    val numericValues = parseIntArray(protectedChars)
    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    sumOfWeightedValues
  }

  private def mgdModCheck(modeDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String): Char = {
    val numericValues = mgdParseIntArray(protectedChars)

    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) {
      case (acc, (value, index)) => acc + (value * weightings(index))
    }

    val modResult = sumOfWeightedValues % modeDivisor
    val expectedChar = remainderMap(modResult)
    expectedChar

  }

  private def mgdParseIntArray(allDigits: String): Array[Int] = {

    val intArray = for (i <- 1 until allDigits.length) yield {
      allDigits.substring(i, i + 1).toInt
    }

    Array(nddsMValue) ++ intArray
  }

  def parseIntArray(allDigits: String): Array[Int] = {
    val ints = new Array[Int](allDigits.length)
    for (i <- ints.indices) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  def eclParseIntArray(allDigits: String): Array[Int] = {
    println(s"*** parseIntArray, allDigits: $allDigits")
    val ints = new Array[Int](allDigits.length)
    ints(0) = E_INT_VAL
    ints(1) = C_INT_VAL
    ints(2) = L_INT_VAL
    for (i <- 3 until ints.length) {
      ints(i) = allDigits.substring(i, i + 1).toInt
    }
    ints
  }

  def safe14ModCheck(modDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String) = {
    val numericValues = safe14ParseIntArray(protectedChars)

    var sumOfWeightedValues = 0
    for (index <- numericValues.indices) {
      sumOfWeightedValues += numericValues(index) * weightings(index)
    }
    val modResult = sumOfWeightedValues % modDivisor
    val expectedChar = remainderMap(modResult)
    expectedChar
  }

  private def safe14ParseIntArray(protectedChars: String) = {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val ints = new Array[Int](protectedChars.length)
    val char1 = protectedChars.substring(0, 1);

//    val tempValue = if (char1.matches("\\d{1}")) char1.toInt else characters.indexOf(protectedChars.charAt(0)) + 33
    if (char1.matches("\\d{1}")) {
      ints(0) = Integer.parseInt(char1)
    } else {
      ints(0) = characters.indexOf(protectedChars.charAt(0)) + 33
    }

    for (i <- 1 until ints.length) {
      ints(i) = protectedChars.substring(i, i + 1).toInt
    }
    ints
  }

}
