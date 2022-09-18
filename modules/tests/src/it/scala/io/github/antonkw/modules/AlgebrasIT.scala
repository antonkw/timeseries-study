package io.github.antonkw.modules

import cats.effect.std.UUIDGen
import cats.effect.{ IO, Resource }
import io.github.antonkw.Generators.now
import io.github.antonkw.algebra.{ MetricsAlgebra, UserEventAlgebra }
import io.github.antonkw.model._
import io.github.antonkw.{ Generators, PostgresSuite, Table }
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session
import weaver.Expectations
import weaver.Expectations.Helpers

import scala.collection.immutable.TreeSet

object AlgebrasIT extends PostgresSuite[Algebras[IO]] {
  implicit lazy val testLogger = Slf4jLogger.getLogger[IO]

  override def makeContext: Resource[IO, Session[IO]] => Algebras[IO] =
    implicit sp => new Algebras[IO](UserEventAlgebra.make[IO](sp), MetricsAlgebra.make[IO](sp))

  override val affectedTables: List[Table] = List(Table.Events)

  //generate batch of event and insert them in parallel
  def batchParallelInsertion(eventAlgebra: UserEventAlgebra[IO]): IO[List[UserEvent]] =
    IO.delay(Generators.eventBigBatchGen.sample.get)
      .flatTap(events =>
        IO.parTraverseN(12)(events)(eventAlgebra.insert).timed.flatMap {
          case (duration, _) => IO.println(s"${events.size} events were inserted in ${duration.toSeconds}s")
        }
      )

  case class MetricsAcc(knownUsers: TreeSet[String], clicks: Int, impressions: Int) {
    def compareWithRetrieved(retrievedMetrics: Metrics): Expectations =
      expect(retrievedMetrics.clicks == clicks) &&
        expect(retrievedMetrics.impressions == impressions) &&
        expect(retrievedMetrics.uniqueUsers == knownUsers.size)
  }

  object MetricsAcc {
    val empty = MetricsAcc(TreeSet(), 0, 0)

    def apply(events: List[UserEvent]): MetricsAcc = events.foldLeft(MetricsAcc.empty) {
      case (acc, event) =>
        val updatedUsers = acc.knownUsers + event.user.value

        event.event match {
          case EventType.Click      => acc.copy(knownUsers = updatedUsers, clicks = acc.clicks + 1)
          case EventType.Impression => acc.copy(knownUsers = updatedUsers, impressions = acc.impressions + 1)
        }
    }
  }

  testWithCleanUpBeforeAndTearDown("basic insertion check")(algebras =>
    batchParallelInsertion(algebras.userEventAlgebra)
      .flatMap(generatedEvents =>
        algebras.metricsAlgebra
          .get(now)
          .map(_.map(MetricsAcc(generatedEvents).compareWithRetrieved).getOrElse(Helpers.failure("No metrics")))
      )
  )

  testWithCleanUpBeforeAndTearDown("appending individual events")(algebras =>
    for {
      _ <- batchParallelInsertion(algebras.userEventAlgebra)
      initialMetrics <- algebras.metricsAlgebra.get(now)
      uuid <- UUIDGen.randomUUID
      _ <- algebras.userEventAlgebra.insert(UserEvent(now, UserId(uuid.toString), EventType.Click))
      metricsAfterClick <- algebras.metricsAlgebra.get(now)
      _ <- algebras.userEventAlgebra.insert(UserEvent(now, UserId(uuid.toString), EventType.Impression))
      metricsAfterImpression <- algebras.metricsAlgebra.get(now)
    } yield expect(metricsAfterClick.get.uniqueUsers == initialMetrics.get.uniqueUsers + 1) &&
      expect(metricsAfterClick.get.clicks == initialMetrics.get.clicks + 1) &&
      expect(metricsAfterImpression.get.impressions == initialMetrics.get.impressions + 1) &&
      expect(metricsAfterImpression.get.uniqueUsers == metricsAfterClick.get.uniqueUsers)
  )
}
