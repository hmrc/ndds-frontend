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

import jakarta.inject.Singleton
import scala.util.Random

trait ReferenceGenerator {
  def generateReference(): String
}

@Singleton
class ReferenceGeneratorImpl extends ReferenceGenerator {
  private val chars  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  private val random = new Random()

  /** Generates a unique 16-character alphanumeric uppercase reference */
  def generateReference(): String =
    (1 to 16).map(_ => chars(random.nextInt(chars.length))).mkString
}
