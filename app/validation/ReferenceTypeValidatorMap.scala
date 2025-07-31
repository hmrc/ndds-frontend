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

import ReferenceTypeValidator.Validator
import models.DirectDebitSource
import models.DirectDebitSource.*

object ReferenceTypeValidatorMap {
  def validatorType(dds: DirectDebitSource): Option[String => Boolean] = {

    val res = dds match {
      case PAYE => summon[Validator[PAYE.type]].validate _
      case MGD => summon[Validator[MGD.type]].validate _
      case SA => summon[Validator[SA.type]].validate _
      case SDLT => summon[Validator[SDLT.type]].validate _
      case VAT => summon[Validator[VAT.type]].validate _
      case NIC => summon[Validator[NIC.type]].validate _
      case OL => summon[Validator[OL.type]].validate _
      case CT => summon[Validator[CT.type]].validate _
      case TC => summon[Validator[TC.type]].validate _
    }

    Some(res)
  }
}
