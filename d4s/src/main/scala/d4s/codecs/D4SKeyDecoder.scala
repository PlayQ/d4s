package d4s.codecs

import java.util.UUID

import cats.syntax.either._
import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValue, DynamoDecoderException}

import scala.util.Try

trait D4SKeyDecoder[T] {
  def decode(item: String): Either[DynamoDecoderException, T]
}

object D4SKeyDecoder {
  implicit val shortKeyDecoder: D4SKeyDecoder[Short] = item =>
    Either.fromTry(Try(item.toShort)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode key $item as Short.", Some(err)))
  implicit val intKeyDecoder: D4SKeyDecoder[Int] = item =>
    Either.fromTry(Try(item.toInt)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode key $item as Int.", Some(err)))
  implicit val uuidKeyDecoder: D4SKeyDecoder[UUID] = item =>
    Either.fromTry(Try(UUID.fromString(item))).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode key $item as UUID.", Some(err)))

  // special case
  implicit val stringKeyDecoder: D4SKeyDecoder[String] = Right(_)
}
