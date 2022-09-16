package io.github.antonkw.algebra

import cats.data.OptionT
import cats.effect.implicits.clockOps
import cats.effect.{ Resource, Sync }
import cats.implicits.{ catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps }
import io.github.antonkw.model.Metrics
import org.typelevel.log4cats.Logger
import skunk.codec.all.timestamp
import skunk.codec.numeric.int8
import skunk.implicits.toStringOps
import skunk.{ Decoder, Query, Session }

import java.time.LocalDateTime
import scala.util.chaining.scalaUtilChainingOps

trait MetricsAlgebra[F[_]] {
  def get(time: LocalDateTime): F[Option[Metrics]]
}

object MetricsAlgebra {
  private object SQL {
    private val metricsDecoder: Decoder[Metrics] =
      (int8 ~ int8 ~ int8 ~ timestamp).gmap[Metrics]

    def getMetrics(time: LocalDateTime): Query[skunk.Void, Metrics] =
      sql"""
           SELECT unique_users, total_clicks, total_impressions, bucket
           FROM time_bucket(INTERVAL '1 hour', TIMESTAMP '#${time.toString}') as target_bucket
               INNER JOIN events_summary_hourly on target_bucket = bucket"""
        .query(metricsDecoder)

  }

  def make[F[_]: Sync: Logger](sessionPool: Resource[F, Session[F]]): MetricsAlgebra[F] =
    (time: LocalDateTime) =>
      sessionPool.use(
        _.execute(SQL.getMetrics(time))
          .map(_.headOption)
          .timed
          .flatMap {
            case (measurement, maybeMetrics) =>
              maybeMetrics
                .pure[F]
                .pipe(OptionT.apply[F, Metrics])
                .semiflatTap(metrics =>
                  Logger[F].info(
                    s"Metrics[${metrics.toShortString}] were queried from bucket[${metrics.bucket}] for [$time] in ${measurement.toMillis}ms"
                  )
                )
                .flatTapNone(Logger[F].info(s"No bucket found for $time").void)
                .value
                .as(maybeMetrics)
          }
      )

}
