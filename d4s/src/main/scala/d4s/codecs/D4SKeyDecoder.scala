package d4s.codecs

import java.util.UUID

import cats.syntax.either._
import d4s.models.DynamoException.DecoderException

import scala.util.Try

trait D4SKeyDecoder[A] {
  def decode(item: String): Either[DecoderException, A]
}

object D4SKeyDecoder {
  @inline def apply[A](implicit ev: D4SKeyDecoder[A]): ev.type = ev

  def decode[A: D4SKeyDecoder](item: String): Either[DecoderException, A] = D4SKeyDecoder[A].decode(item)

  implicit val stringKeyDecoder: D4SKeyDecoder[String] = Right(_)
  implicit val byteKeyDecoder: D4SKeyDecoder[Byte]     = tryKeyDecoder("Byte")(_.toByte)
  implicit val shortKeyDecoder: D4SKeyDecoder[Short]   = tryKeyDecoder("Short")(_.toShort)
  implicit val intKeyDecoder: D4SKeyDecoder[Int]       = tryKeyDecoder("Int")(_.toInt)
  implicit val longKeyDecoder: D4SKeyDecoder[Long]     = tryKeyDecoder("Long")(_.toLong)
  implicit val uuidKeyDecoder: D4SKeyDecoder[UUID]     = tryKeyDecoder("UUID")(UUID.fromString)

  def tryKeyDecoder[A](name: String)(f: String => A): D4SKeyDecoder[A] =
    item => Either.fromTry(Try(f(item))).leftMap(err => DecoderException(s"Cannot decode key $item as $name", Some(err)))
}
