package io.github.antonkw.model

import enumeratum.EnumEntry.Lowercase
import skunk.Codec
import skunk.codec.all.{ `enum`, text, timestamp }
import skunk.data.Type

sealed trait EventType extends enumeratum.EnumEntry with Lowercase

object EventType extends enumeratum.Enum[EventType] {
  val values: scala.IndexedSeq[EventType] = findValues

  case object Click extends EventType
  case object Impression extends EventType
}

case class UserEvent(eventTime: EventTimestamp, user: UserId, event: EventType) {
  def toShortString = s"$user/$event"
}

object UserEvent {
  implicit val codec: Codec[UserEvent] =
    (timestamp.imap(EventTimestamp.apply)(_.value) ~ text.imap[UserId](UserId.apply)(_.value) ~ enum[EventType](
          EventType,
          Type("event_type")
        ))
      .gimap[UserEvent]
}
