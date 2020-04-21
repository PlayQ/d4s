package d4s.codecs

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

  abstract class DynamoDecoderException(message: String, cause: Option[Throwable]) extends RuntimeException(message, cause.orNull) {
    def union(that: DynamoDecoderException): DynamoDecoderException = {
      val errorLog = message + "\n" + that.getMessage
      new DynamoDecoderException(errorLog, None) {}
    }
  }

  final class CannotDecodeAttributeValue(val msg: String, val cause: Option[Throwable]) extends DynamoDecoderException(msg, cause)
  final class CannotDecodeKeyValue(val msg: String, val cause: Option[Throwable]) extends DynamoDecoderException(msg, cause)
}
