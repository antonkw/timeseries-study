package io.github.antonkw

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops.toCoercibleIdOps
import sttp.tapir.{Codec, CodecFormat}

package object ext {

  implicit def codecForNewType[A, B, CF <: CodecFormat](implicit
      ev1: Coercible[A, B],
      c: Codec[String, A, CF],
      ev2: Coercible[B, A]
  ): Codec[String, B, CF] =
    c.map(_.coerce[B])(_.coerce[A])
}
