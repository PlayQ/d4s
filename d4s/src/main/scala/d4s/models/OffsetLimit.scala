package d4s.models

import d4s.codecs.{D4SDecoder, D4SEncoder}

final case class OffsetLimit(offset: Int, limit: Short)

object OffsetLimit {
  implicit val enc: D4SEncoder[OffsetLimit] = D4SEncoder.derived
  implicit val dec: D4SDecoder[OffsetLimit] = D4SDecoder.derived
}
