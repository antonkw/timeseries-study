package io.github.antonkw

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops.toCoercibleIdOps
import sttp.tapir.CodecFormat

package object ext {
  implicit class CodecOps[B](codec: skunk.Codec[B]) {
    def cimap[A: Coercible[B, *]](implicit ev: Coercible[A, B]): skunk.Codec[A] =
      codec.imap(_.coerce[A])((ev(_)))
  }

  implicit def codecForNewType[A, B, CF <: CodecFormat](
      implicit
      ev1: Coercible[A, B],
      c: sttp.tapir.Codec[String, A, CF],
      ev2: Coercible[B, A]
  ): sttp.tapir.Codec[String, B, CF] =
    c.map(_.coerce[B])(_.coerce[A])
}
