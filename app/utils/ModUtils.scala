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
import scala.annotation.tailrec

object ModUtils {

  private val nddsMValue = 45
  val charRemainderMap: Array[Char] = Array(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'X', 'J', 'K', 'L', 'M', 'N', 'Y', 'P', 'Q', 'R', 'S', 'T', 'Z', 'V', 'W'
  )
  private val E_INT_VAL = 37
  private val C_INT_VAL = 35
  private val L_INT_VAL = 44

  val SAFE_14_REF_FORMAT: Pattern = Pattern.compile("X[A-Z][A-Z0-9]\\d{11}")
  val SAFE_15_REF_FORMAT: Pattern = Pattern.compile("X[A-Z]\\d{13}")
  val SAFE_ECL_REF_FORMAT: Pattern = Pattern.compile("X[A-Z]ECL\\d{10}")

  def modTCRef(reference: String) = {
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

  def modSafe14(reference: String) = {
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 14
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2)
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = safe14ModCheck(modDivisor, weightings, charRemainderMap, protectedChars)
    checkChar == expectedChar
  }

  def modSafe15(reference: String) = {
    val modDivisor = 23
    val indexOfCheckChar = 1
    val indexOfFirstProtectedChar = 2
    val indexOfLastProtectedChar = 15
    val weightings = Array(9, 10, 11, 12, 13, 8, 7, 6, 5, 4, 3, 2, 1)
    val checkChar = reference.charAt(indexOfCheckChar)
    val protectedChars = reference.substring(indexOfFirstProtectedChar, indexOfLastProtectedChar)
    val expectedChar = modCheck(modDivisor, weightings, charRemainderMap, parseIntArray(protectedChars))
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
      case _             => false
    }
  }

  @tailrec
  private def modCheckForVat(modDivisor: Int, sumOfWeightedValues: Int): Int = {
    if (sumOfWeightedValues > modDivisor) {
      modCheckForVat(modDivisor, sumOfWeightedValues - modDivisor)
    } else {
      if (sumOfWeightedValues <= modDivisor) {
        modDivisor - sumOfWeightedValues
      } else {
        sumOfWeightedValues
      }
    }
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

  private def modCheckTotal(weightings: Array[Int], protectedChar: String): Int = {
    val numericValues = parseIntArray(protectedChar)
    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) { case (acc, (value, index)) =>
      acc + (value * weightings(index))
    }
    sumOfWeightedValues
  }

  private def mgdModCheck(modeDivisor: Int, weightings: Array[Int], remainderMap: Array[Char], protectedChars: String): Char = {
    val numericValues = mgdParseIntArray(protectedChars)
    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) { case (acc, (value, index)) =>
      acc + (value * weightings(index))
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
    val sumOfWeightedValues = numericValues.zipWithIndex.foldLeft(0) { case (acc, (value, index)) =>
      acc + (value * weightings(index))
    }
    val modResult = sumOfWeightedValues % modDivisor
    remainderMap(modResult)
  }

  private def safe14ParseIntArray(protectedChars: String) = {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val char1 = protectedChars.take(1)
    val tempValue = if (char1.matches("\\d{1}")) char1.toInt else characters.indexOf(protectedChars.charAt(0)) + 33
    val intArray = for (i <- 1 until protectedChars.length) yield {
      protectedChars.substring(i, i + 1).toInt
    }
    Array(tempValue) ++ intArray
  }
}
