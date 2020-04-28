package d4s.codecs

trait D4SAttributeEncoderScala213 {

  implicit def literalTupleEncoder[S <: String, V: D4SAttributeEncoder]: D4SEncoder[(S, V)] = {
    case (k, v) => D4SAttributeEncoder.encodePlain(k, v)
  }

}
