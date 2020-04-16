package d4s.codecs

import java.util.UUID

trait D4SKeyEncoder[T] {
  def encode(item: T): String
}

object D4SKeyEncoder {
//  implicit val shortKeyEncoder: D4SKeyEncoder[Short] = _.toString
//  implicit val intKeyEncoder: D4SKeyEncoder[Int]     = _.toString
//  implicit val uuidKeyEncoder: D4SKeyEncoder[UUID]   = _.toString
  implicit def simpleTypeEncoder[T]: D4SKeyEncoder[T] = _.toString
}
