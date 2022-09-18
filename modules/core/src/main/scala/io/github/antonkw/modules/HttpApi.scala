package io.github.antonkw.modules

import cats.effect.Async
import io.github.antonkw.http.AnalyticsEndpoints
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final class HttpApi[F[_]: Async] private (
    analyticsEndpoints: AnalyticsEndpoints[F]
) {
  import analyticsEndpoints._

  val swagger = SwaggerInterpreter().fromServerEndpoints[F](List(metrics, insertion), "Time-series Analytics", "0.1")

  val routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(insertion :: metrics :: swagger)
}

object HttpApi {
  def make[F[_]: Async](analyticsEndpoints: AnalyticsEndpoints[F]) = new HttpApi[F](analyticsEndpoints)
}
