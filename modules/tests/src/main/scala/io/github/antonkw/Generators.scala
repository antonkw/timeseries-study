package io.github.antonkw

import io.github.antonkw.model.EventType.{ Click, Impression }
import io.github.antonkw.model.{ EventTimestamp, EventType, UserEvent, UserId }
import org.scalacheck.Gen

import java.sql.Timestamp
import java.time.{ LocalDateTime, OffsetDateTime }

object Generators {
  val BatchSize = 1000

  val genUserId: Gen[UserId] = {
    Gen.frequency(
      (3, Gen.oneOf("user1", "user2", "user3").map(UserId.apply)), //some duplicates
      (
        9,
        Gen
          .chooseNum(1, 50)
          .flatMap(n => Gen.buildableOfN[String, Char](n, Gen.alphaNumChar))
          .map(UserId.apply)
      )
    )
  }

  val genEventType: Gen[EventType] = Gen.frequency((4, Impression), (1, Click)) //typically there are more impressions

  lazy val now = EventTimestamp(LocalDateTime.now())
  val offset   = OffsetDateTime.now()

  val currentHour = Gen
    .const(now)
    .flatMap(dt =>
      Gen.chooseNum(
        dt.value.withMinute(0).toEpochSecond(OffsetDateTime.now().getOffset) * 1000,
        dt.value.withMinute(59).toEpochSecond(OffsetDateTime.now().getOffset) * 1000
      )
    )
    .map(dt => EventTimestamp(new Timestamp(dt).toLocalDateTime))

  val eventGen: Gen[UserEvent] =
    for {
      userId <- genUserId
      eventType <- genEventType
      eventTimestamp <- currentHour
    } yield UserEvent(eventTimestamp, userId, eventType)

  val eventBigBatchGen = Gen.listOfN(BatchSize, Generators.eventGen)

}
