package io.github.antonkw

import cats.effect.{ ExitCode, IO, IOApp, Sync }
import ciris.Secret
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import io.github.antonkw.config.TimescaleConfig
import io.github.antonkw.http.AnalyticsEndpoints
import io.github.antonkw.modules.{ Algebras, HttpApi }
import io.github.antonkw.resources.AppResources
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.Console.{ GREEN, RESET }

object Main extends IOApp {
  val timescaleConfig = TimescaleConfig(
    "0.0.0.0",
    5432,
    "timescale",
    Secret("p_318Jhs&2"),
    "eventstore",
    4
  )

  val port: UserPortNumber = 8080
  val host: NonEmptyString = "localhost"

  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] =
    AppResources
      .make[IO](timescaleConfig)
      .use(resources =>
        Algebras
          .make(resources)
          .flatTap(_ => IO.println(s"Checkout ${RESET}${GREEN}Swagger${RESET} at http://$host:$port/docs"))
          .map(algebras => HttpApi.make[IO](new AnalyticsEndpoints[IO](algebras)).routes)
          .flatMap(routes =>
            IO.executionContext.flatMap(ec =>
              BlazeServerBuilder[IO]
                .withExecutionContext(ec)
                .bindHttp(port.value, host.value)
                .withHttpApp(Router("/" -> routes).orNotFound)
                .serve
                .compile
                .drain
            )
          )
      )
      .as(ExitCode.Success)
}
