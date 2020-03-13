package d4s.codecs

import io.circe.Json
import magnolia.Magnolia

import scala.reflect.macros.whitebox

object CodecsUtils {

  object CastedMagnolia {
    def genWithCast[T: c.WeakTypeTag, R: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
      import c.universe._
      val resultType = appliedType(weakTypeOf[R], weakTypeOf[T])
      q"${Magnolia.gen[T](c)}.asInstanceOf[$resultType]"
    }
  }

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
