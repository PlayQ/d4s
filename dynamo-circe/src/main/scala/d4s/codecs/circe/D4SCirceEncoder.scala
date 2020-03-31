package d4s.codecs.circe

import d4s.codecs.D4SEncoder
import io.circe._
import io.circe.syntax._

object D4SCirceEncoder {
  def derived[T: Encoder.AsObject]: D4SEncoder[T] = {
    _.asJsonObject.toMap.map { case (k, v) => k -> D4SCirceAttributeEncoder.jsonToAttribute(v) }
  }
}
