package d4s.codecs.circe

import d4s.codecs.D4SCodec
import io.circe.{Decoder, Encoder}

object D4SCirceCodec {
  def derived[T: Encoder.AsObject: Decoder]: D4SCodec[T] = D4SCodec.fromPair(D4SCirceEncoder.derived, D4SCirceDecoder.derived)
}
