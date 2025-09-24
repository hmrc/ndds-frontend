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

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MacGenerator {

  private val Algorithm = "HmacSHA1"

  def generateMac(
                   accountName: String,
                   accountNumber: String,
                   sortCode: String,
                   addressLine1: String,
                   addressLine2: String,
                   postcode: String,
                   bankName: String,
                   bacsNumber: String,
                   secretKey: String
                 ): String = {
    // 1. Build key=value string in defined order
    val dataString =
      s"accountName=$accountName" +
        s"accountNumber=$accountNumber" +
        s"sortCode=$sortCode" +
        s"addressLine1=$addressLine1" +
        s"addressLine2=$addressLine2" +
        s"postcode=$postcode" +
        s"bankName=$bankName" +
        s"bacsNumber=$bacsNumber"

    // 2. Convert to UTF-8 bytes
    val bytes = dataString.getBytes("UTF-8")

    // 3. Init HMAC with secret key
    val secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), Algorithm)
    val mac = Mac.getInstance(Algorithm)
    mac.init(secretKeySpec)

    // 4. Generate and Base64 encode
    val rawMac = mac.doFinal(bytes)
    Base64.getEncoder.encodeToString(rawMac)
  }
}
