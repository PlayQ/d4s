package d4s.codecs

trait D4SDecoderScala213 {

  implicit def literalTupleDecoder[K <: String: ValueOf, V: D4SDecoder]: D4SDecoder[(K, V)] = D4SDecoder.objectDecoder {
    D4SDecoder.decodePlain[V](valueOf, _).map(valueOf -> _)
  }

}
