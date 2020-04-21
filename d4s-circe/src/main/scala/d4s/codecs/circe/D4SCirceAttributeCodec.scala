package d4s.codecs.circe

import d4s.codecs.D4SAttributeCodec
import io.circe.{Decoder, Encoder}

object D4SCirceAttributeCodec {
  def derived[T: Encoder: Decoder]: D4SAttributeCodec[T] = D4SAttributeCodec.fromPair(D4SCirceAttributeEncoder.derived, D4SCirceDecoder.derived)
}
