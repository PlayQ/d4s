package d4s.codecs

import java.util.UUID

import cats.syntax.either._
import d4s.models.DynamoException.DecoderException

import scala.util.Try

trait D4SKeyDecoder[T] {
  def decode(item: String): Either[DecoderException, T]
}

object D4SKeyDecoder {
  implicit val shortKeyDecoder: D4SKeyDecoder[Short] = item =>
    Either.fromTry(Try(item.toShort)).leftMap(err => DecoderException(s"Cannot decode key $item as Short.", Some(err)))
  implicit val intKeyDecoder: D4SKeyDecoder[Int] = item =>
    Either.fromTry(Try(item.toInt)).leftMap(err => DecoderException(s"Cannot decode key $item as Int.", Some(err)))
  implicit val uuidKeyDecoder: D4SKeyDecoder[UUID] = item =>
    Either.fromTry(Try(UUID.fromString(item))).leftMap(err => DecoderException(s"Cannot decode key $item as UUID.", Some(err)))

  // special case
  implicit val stringKeyDecoder: D4SKeyDecoder[String] = Right(_)
}
