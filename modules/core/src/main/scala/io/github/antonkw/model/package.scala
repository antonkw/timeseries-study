package io.github.antonkw

import io.estatico.newtype.macros.newtype

import java.time.LocalDateTime

package object model {
  @newtype case class EventTimestamp(value: LocalDateTime)

  @newtype case class UserId(value: String)
}
