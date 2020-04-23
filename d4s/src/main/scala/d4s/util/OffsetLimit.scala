package d4s.util

import d4s.codecs.{D4SDecoder, D4SEncoder}

final case class OffsetLimit(offset: Int, limit: Short)

object OffsetLimit {
  implicit val enc: D4SEncoder[OffsetLimit] = D4SEncoder.nonСastedGen[OffsetLimit].asInstanceOf[D4SEncoder[OffsetLimit]]
  implicit val dec: D4SDecoder[OffsetLimit] = D4SDecoder.derived[OffsetLimit]
}
