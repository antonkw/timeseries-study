package io.github.antonkw.http

import cats.effect.Async
import cats.implicits.{ catsSyntaxEitherId, toFunctorOps }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{ Greater, Less }
import eu.timepit.refined.string.MatchesRegex
import io.estatico.newtype.macros.newtype
import io.github.antonkw.ext._
import io.github.antonkw.http.AnalyticsEndpoints.{ eventInsertionEndpoint, metricsEndpoint, InsertionQueryParams }
import io.github.antonkw.model.{ EventTimestamp, EventType, Metrics, UserEvent, UserId }
import io.github.antonkw.modules.Algebras
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.codec.enumeratum._
import sttp.tapir.codec.refined._
import sttp.tapir.server.ServerEndpoint.Full

import java.sql.Timestamp

object AnalyticsEndpoints {
  val Path: String = "analytics"

  type UserIdPred         = String Refined MatchesRegex["^[a-zA-Z0-9_-]{1,50}$"] // 1 to 50 chars (letters in both registers, numbers, -, _)
  type EventTimestampPred = Long Refined And[Greater[1262300400L], Less[7289564400L]] //2010-01-01 to 2200-12-31 00:00:00

  @newtype case class EventTimestampQueryParam(value: EventTimestampPred) {
    def toDomain: EventTimestamp = EventTimestamp(new Timestamp(value.value * 1000).toLocalDateTime)
  }

  @newtype case class UserIdQueryParam(value: UserIdPred) {
    def toDomain: UserId = UserId(value.value)
  }

  val metricsOutput: EndpointIO.Body[String, String] =
    stringBody
      .description("Report for the requested hour")
      .example(
        """|unique_users,2
         |clicks,12
         |impressions,42""".stripMargin
      )

  val metricsEndpoint: Endpoint[Unit, EventTimestampQueryParam, String, String, Any] =
    endpoint.get
      .in(Path)
      .in(
        query[EventTimestampQueryParam]("timestamp")
          .description("Timestamp (count of seconds from 1970) that will be use as reference for particular hour")
      )
      .description("Check out metrics")
      .errorOut(stringBody)
      .out(metricsOutput)

  type InsertionQueryParams = (EventTimestampQueryParam, UserIdQueryParam, EventType)

  val eventInsertionEndpoint: Endpoint[Unit, InsertionQueryParams, String, Unit, Any] =
    endpoint.post
      .in(Path)
      .in(
        query[EventTimestampQueryParam]("timestamp").description(
          "Timestamp that will be use as reference for particular hour, 2010 to 2200 years are allowed"
        )
      )
      .in(query[UserIdQueryParam]("user").description("User ID; 1 to 50 length; letters, digits, '_', and '_' are allowed"))
      .in(query[EventType]("event").description("click or impression"))
      .description("Submit new event")
      .errorOut(stringBody)
      .out(statusCode(StatusCode.NoContent))
}

final class AnalyticsEndpoints[F[_]: Async](algebras: Algebras[F]) {
  val metrics: Full[Unit, Unit, AnalyticsEndpoints.EventTimestampQueryParam, String, String, Any, F] =
    metricsEndpoint.serverLogic(timestamp =>
      algebras.metricsAlgebra
        .get(timestamp.toDomain)
        .fmap(
          _.map(_.toReport)
            .getOrElse(Metrics.emptyReport)
            .asRight[String]
        )
    )

  val insertion: Full[Unit, Unit, InsertionQueryParams, String, Unit, Any, F] =
    eventInsertionEndpoint.serverLogic {
      case (timestamp, user, eventType) =>
        val event = UserEvent(timestamp.toDomain, user.toDomain, eventType)
        algebras.userEventAlgebra.insert(event).void.fmap(_.asRight)
    }
}
