package io.github.antonkw.http

import cats.effect.Async
import cats.implicits.{ catsSyntaxEitherId, toFunctorOps }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{ Greater, Less }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.github.antonkw.ext._
import io.github.antonkw.http.AnalyticsEndpoints.{ eventInsertionEndpoint, metricsEndpoint, InsertionQueryParams }
import io.github.antonkw.model.{ EventType, Metrics, UserEvent }
import io.github.antonkw.modules.Algebras
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.codec.refined._
import sttp.tapir.server.ServerEndpoint.Full

import java.sql.Timestamp

object AnalyticsEndpoints {
  val Path: String = "analytics"

  type EventTimestampPred = Long Refined And[Greater[1262300400L], Less[7289564400L]] //2010-01-01 to 2200-12-31 00:00:00

  @newtype case class EventTimestamp(value: EventTimestampPred)
  @newtype case class UserId(value: NonEmptyString)

  val metricsOutput: EndpointIO.Body[String, String] =
    stringBody
      .description("Report for the requested hour")
      .example(
        """|unique_users,2
         |clicks,12
         |impressions,42""".stripMargin
      )

  val metricsEndpoint: Endpoint[Unit, EventTimestamp, String, String, Any] =
    endpoint.get
      .in(Path)
      .in(
        query[EventTimestamp]("timestamp")
          .description("Timestamp (count of seconds from 1970) that will be use as reference for particular hour")
      )
      .description("Check out metrics")
      .errorOut(stringBody)
      .out(metricsOutput)

  type InsertionQueryParams = (EventTimestamp, UserId, EventType)
  val eventInsertionEndpoint: Endpoint[Unit, InsertionQueryParams, String, Unit, Any] =
    endpoint.post
      .in(Path)
      .in(
        query[EventTimestamp]("timestamp")
          .description("Timestamp (count of seconds from 1970) that will be use as reference for particular hour")
      )
      .in(query[UserId]("user").description("User ID"))
      .in(
        query[EventType]("event")
          .description("click or impression")
      )
      .description("Submit new event")
      .errorOut(stringBody)
      .out(statusCode(StatusCode.NoContent))
}

final class AnalyticsEndpoints[F[_]: Async](algebras: Algebras[F]) {
  val metrics: Full[Unit, Unit, AnalyticsEndpoints.EventTimestamp, String, String, Any, F] =
    metricsEndpoint.serverLogic(timestamp =>
      algebras.metricsAlgebra
        .get(new Timestamp(timestamp.value.value * 1000).toLocalDateTime)
        .fmap(
          _.map(_.toReport)
            .getOrElse(Metrics.emptyReport)
            .asRight[String]
        )
    )

  val insertion: Full[Unit, Unit, InsertionQueryParams, String, Unit, Any, F] =
    eventInsertionEndpoint.serverLogic {
      case (timestamp, user, eventType) =>
        val event = UserEvent(new Timestamp(timestamp.value.value * 1000).toLocalDateTime, user.value.value, eventType)
        algebras.userEventAlgebra.insert(event).void.fmap(_.asRight)
    }
}
