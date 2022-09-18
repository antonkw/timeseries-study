package io.github.antonkw.modules

import cats.Parallel
import cats.effect.kernel.{ Async, Clock }
import io.github.antonkw.algebra.{ MetricsAlgebra, UserEventAlgebra }
import io.github.antonkw.resources.AppResources
import org.typelevel.log4cats.Logger

final class Algebras[F[_]] private[modules] (
    val userEventAlgebra: UserEventAlgebra[F],
    val metricsAlgebra: MetricsAlgebra[F]
)

object Algebras {

  import cats.implicits.{ toFlatMapOps, toFunctorOps }

  def make[F[_]: Clock: Async: Logger: Parallel](appResources: AppResources[F]): F[Algebras[F]] =
    for {
      _ <- Logger[F].info(s"Init algebras has started")
      userEventAlgebra = UserEventAlgebra.make(appResources.session)
      metricsAlgebra   = MetricsAlgebra.make(appResources.session)
      _ <- Logger[F].info(s"Init algebras has completed")
    } yield new Algebras[F](userEventAlgebra, metricsAlgebra)

}
