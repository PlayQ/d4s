package d4s.codecs

trait AttributeNamesScala213 {

  implicit def literalTupleAttributeNames[K <: String: ValueOf, V]: AttributeNames[(K, V)] = {
    AttributeNames(Set[String](valueOf))
  }

}
