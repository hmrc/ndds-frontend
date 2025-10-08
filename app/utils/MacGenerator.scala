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

import config.FrontendAppConfig

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class MacGenerator @Inject() (appConfig: FrontendAppConfig) {

  private val Algorithm = "HmacSHA1"

  def generateMac(
    accountName: String,
    accountNumber: String,
    sortCode: String,
    lines: Seq[String],
    town: String,
    postcode: String,
    bankName: String,
    bacsNumber: String
  ): String = {

    val dataString =
      Seq(
        accountName,
        accountNumber,
        sortCode,
        lines.mkString(" "),
        town,
        postcode,
        bankName,
        bacsNumber
      ).mkString("&")

    val bytes = dataString.getBytes("UTF-8")
    val secretKeyBytes = Base64.getDecoder.decode(appConfig.macKey)
    val secretKeySpec = new SecretKeySpec(secretKeyBytes, Algorithm)

    val mac = Mac.getInstance(Algorithm)
    mac.init(secretKeySpec)

    val rawMac = mac.doFinal(bytes)
    Base64.getEncoder.encodeToString(rawMac)
  }
}
