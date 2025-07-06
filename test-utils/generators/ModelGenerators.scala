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

package generators

import models._
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  implicit lazy val arbitraryYourBankDetails: Arbitrary[YourBankDetails] =
    Arbitrary {
      for {
        accountHolderName <- Gen.alphaStr.suchThat(_.nonEmpty)
        sortCode <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
        accountNumber <- Gen.listOfN(8, Gen.numChar).map(_.mkString)
      } yield YourBankDetails(accountHolderName, sortCode, accountNumber)
    }

  implicit lazy val arbitraryPersonalOrBusinessAccount: Arbitrary[PersonalOrBusinessAccount] =
    Arbitrary {
      Gen.oneOf(PersonalOrBusinessAccount.values.toSeq)
    }
}
