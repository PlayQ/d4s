package d4s.codecs.circe

import d4s.codecs.CodecsUtils.DynamoDecoderException
import io.circe.Json

object utils {
  final class CannotDecodeAttributeValueAsJson(val item: String)
    extends DynamoDecoderException(
      s"Couldn't decode dynamo item=$item as Json. A case wasn't handled in DynamoDecoder.attributeToJson",
      None
    )

  final class CirceDecodeException(val item: String, json: Json, cause: io.circe.Error)
    extends DynamoDecoderException(
      s"Circe error when decoding item=$item json=$json: ${cause.getMessage}",
      Some(cause)
    )
}
