package d4s.codecs

trait D4SKeyEncoder[T] {
  def encode(item: T): String
}

object D4SKeyEncoder {
  implicit def simpleTypeEncoder[T]: D4SKeyEncoder[T] = _.toString
}
