package d4s.util

import io.circe.{Codec, derivation}

final case class OffsetLimit(offset: Int, limit: Short)

object OffsetLimit {
  implicit val offsetLimitCodec: Codec[OffsetLimit] = derivation.deriveCodec[OffsetLimit]
}
