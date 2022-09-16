package io.github.antonkw.resources

import cats.effect.std.Console
import cats.effect.{ Concurrent, Resource }
import cats.syntax.all._
import fs2.io.net.Network
import io.github.antonkw.config.TimescaleConfig
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import skunk.codec.text._
import skunk.implicits._
import skunk.{ Decoder, Session, SessionPool, Strategy }

case class AppResources[F[_]](
    session: Resource[F, Session[F]]
)

object AppResources {
  def make[F[_]: Concurrent: Network: Console: Logger](
      config: TimescaleConfig
  ): Resource[F, AppResources[F]] = timescale[F](config).map(AppResources.apply[F])

  def timescale[F[_]: Concurrent: Network: Console: Logger](
      config: TimescaleConfig
  ): SessionPool[F] = {
    def healthcheck(
        r: Resource[F, Session[F]]
    ): F[Unit] = {
      case class BasicInfo(version: String, user: String, db: String)
      val basicInfoDecoder: Decoder[BasicInfo] = (text ~ name ~ name).gmap[BasicInfo]

      r.use { session =>
        session
          .unique(sql"select version(), session_user, current_database();".query(basicInfoDecoder)) >>= (basicInfo =>
          Logger[F].info(s"Connected to Timescale (user[${basicInfo.user}], db[${basicInfo.db}]): ${basicInfo.version}")
        )
      }
    }

    Session
      .pooled[F](
        host = config.host.value,
        port = config.port.value,
        user = config.user.value,
        password = Some(config.password.value.value),
        database = config.database.value,
        max = config.max.value,
        strategy = Strategy.SearchPath
      )
      .evalTap(healthcheck)
  }

}
