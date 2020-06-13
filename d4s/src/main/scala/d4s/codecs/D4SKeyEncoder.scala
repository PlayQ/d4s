package d4s.codecs

import java.util.UUID

trait D4SKeyEncoder[A] {
  def encode(item: A): String
}

object D4SKeyEncoder {
  @inline def apply[A](implicit ev: D4SKeyEncoder[A]): ev.type = ev

  def encode[A: D4SKeyEncoder](item: A): String = D4SKeyEncoder[A].encode(item)

  implicit val stringKeyEncoder: D4SKeyEncoder[String] = identity
  implicit val byteKeyEncoder: D4SKeyEncoder[Byte]     = _.toString
  implicit val shortKeyEncoder: D4SKeyEncoder[Short]   = _.toString
  implicit val intKeyEncoder: D4SKeyEncoder[Int]       = _.toString
  implicit val longKeyEncoder: D4SKeyEncoder[Long]     = _.toString
  implicit val uuidKeyEncoder: D4SKeyEncoder[UUID]     = _.toString
}
