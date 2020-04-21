package d4s.util

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, D4SEncoder}

final case class OffsetLimit(offset: Int, limit: Short)

object OffsetLimit {
  implicit val enc: D4SAttributeEncoder[OffsetLimit] = D4SAttributeEncoder.derived[OffsetLimit]
  implicit val dec: D4SDecoder[OffsetLimit] = D4SDecoder.derived[OffsetLimit]
}
