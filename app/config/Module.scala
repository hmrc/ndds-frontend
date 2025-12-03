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

package config

import controllers.actions.*
import play.api.inject.{Binding, Module as PlayModule}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.time.{Clock, ZoneOffset}

class Module extends PlayModule {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[DataRetrievalAction].to(classOf[DataRetrievalActionImpl]),
    bind[DataRequiredAction].to[DataRequiredActionImpl],
    bind[FrontendAppConfig].toSelf,
    bind[Encrypter with Decrypter].toProvider[CryptoProvider],
    // For session based storage instead of persistent, change to SessionIdentifierAction
    bind[IdentifierAction].to[AuthenticatedIdentifierAction],
    bind[Clock].toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
  )
}
