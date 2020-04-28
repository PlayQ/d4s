package d4s.codecs

trait D4SAttributeEncoderScala213 {

  implicit def literalTupleEncoder[V: D4SAttributeEncoder]: D4SEncoder[(String, V)] = {
    case (k, v) => D4SAttributeEncoder.encodePlain(k, v)
  }

}
