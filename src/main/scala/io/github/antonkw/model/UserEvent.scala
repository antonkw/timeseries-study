package io.github.antonkw.model

import enumeratum.EnumEntry.Lowercase
import skunk.Codec
import skunk.codec.all.{ `enum`, text, timestamp }
import skunk.data.Type

import java.time.LocalDateTime

sealed trait EventType extends enumeratum.EnumEntry with Lowercase

object EventType extends enumeratum.Enum[EventType] {
  val values: scala.IndexedSeq[EventType] = findValues

  case object Click extends EventType
  case object Impression extends EventType
}

case class UserEvent(eventTime: LocalDateTime, user: String, event: EventType) {
  def toShortString = s"$user/$event"
}

object UserEvent {
  implicit val codec: Codec[UserEvent] =
    (timestamp ~ text ~ enum[EventType](EventType, Type("event_type"))).gimap[UserEvent]
}
