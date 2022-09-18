package io.github.antonkw.algebra

import cats.effect.implicits.clockOps
import cats.effect.{ Resource, Sync }
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import io.github.antonkw.model.{ EventTimestamp, EventType, UserEvent, UserId }
import org.typelevel.log4cats.Logger
import skunk.codec.all.{ `enum`, text, timestamp, uuid }
import skunk.data.Type
import skunk.implicits.toStringOps
import skunk.{ Encoder, Query, Session }

import java.util.UUID

trait UserEventAlgebra[F[_]] {
  def insert(event: UserEvent): F[UUID]
}

object UserEventAlgebra {
  private object SQL {
    private val eventEncoder: Encoder[UserEvent] =
      (
        timestamp.imap(EventTimestamp.apply)(_.value)
          ~ text.imap[UserId](UserId.apply)(_.value)
          ~ enum[EventType](EventType, Type("event_type"))
      ).gcontramap[UserEvent]

    val insertUser: Query[UserEvent, UUID] =
      sql"INSERT INTO event_history (event_time, username, event_type) VALUES ($eventEncoder) RETURNING id".query(uuid)

  }

  def make[F[_]: Sync: Logger](sessionPool: Resource[F, Session[F]]): UserEventAlgebra[F] =
    (event: UserEvent) =>
      sessionPool.use(
        _.prepare(SQL.insertUser).use(
          _.unique(event).timed.flatMap {
            case (measurement, id) =>
              Logger[F].info(s"Inserted ${event.toShortString} with Id[$id] in ${measurement.toMillis}ms").as(id)
          }
        )
      )

}
