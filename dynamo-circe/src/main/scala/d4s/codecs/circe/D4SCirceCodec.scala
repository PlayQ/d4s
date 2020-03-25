package d4s.codecs.circe

import d4s.codecs.{D4SCodec, D4SDecoder, D4SEncoder}
import io.circe.{Decoder, Encoder}

trait D4SCirceCodec[T] extends D4SCodec[T]

object D4SCirceCodec {
  def derive[T: Encoder.AsObject: Decoder]: D4SCirceCodec[T] = new D4SCirceCodec[T] {
    override def encoder: D4SEncoder[T] = D4SCirceEncoder.derived
    override def decoder: D4SDecoder[T] = D4SCirceDecoder.derived
  }
}
