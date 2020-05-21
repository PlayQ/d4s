package d4s.codecs

import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

final class LiteralCodecsTest extends AnyWordSpec with Checkers {

  "literal tuple encode/decode test" in check(Prop.forAllNoShrink {
    i: Int =>
      val t: ("name", Int) = ("name", i)
      val attrMap          = D4SEncoder.encodeObject(t)
      val attrMap1         = D4SEncoder.encodeObject(("name", i))

      D4SDecoder.decodeObject[("name", Int)](attrMap) == Right(t) &&
      D4SDecoder.decodeObject[("name", Int)](attrMap1) == Right(t) &&
      attrMap.size == 1 && attrMap.head._1 == t._1 && attrMap.head._2.n().toInt == i
  })

}
