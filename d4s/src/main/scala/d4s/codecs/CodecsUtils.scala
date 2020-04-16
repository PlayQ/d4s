package d4s.codecs

import io.circe.Json

object CodecsUtils {

  sealed abstract class DynamoDecoderException(message: String, cause: Option[Throwable]) extends RuntimeException(message, cause.orNull)
  final class CannotDecodeAttributeValue(val msg: String, val cause: Option[Throwable]) extends DynamoDecoderException(msg, cause)

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
