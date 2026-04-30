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

package testOnly.utils

import utils.ClockProvider

import java.time.Clock
import javax.inject.Singleton

@Singleton
class TestOnlyClockProvider extends ClockProvider {

  private val defaultClock = Clock.systemUTC()

  private var _clock = defaultClock

  def clock: Clock = _clock

  def setClock(newClock: Clock): Unit = {
    _clock = newClock
  }

  def resetClock(): Unit = {
    _clock = defaultClock
  }

}
