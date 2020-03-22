package d4s.codecs

import d4s.codecs.circe.{DynamoDecoder, DynamoEncoder}
import d4s.env.DynamoRnd
import io.circe.{Codec, derivation}
import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

@SuppressWarnings(Array("EitherGet", "FinalModifierOnCaseClass"))
class DynamoCodecTest extends AnyWordSpec with Checkers with DynamoRnd {

  case class TestNoDouble(
    a: Int,
    b: Long,
    c: Boolean,
    d: String,
    e: Seq[Int],
    f: Option[Unit],
    m: Map[String, Int]
  )
  object TestNoDouble {
    implicit val codec: Codec.AsObject[TestNoDouble] = derivation.deriveCodec[TestNoDouble]
  }

  case class TestByteArray(a: Array[Byte])

  case class TestDouble(a: Double)
  object TestDouble {
    implicit val codec: Codec.AsObject[TestDouble] = derivation.deriveCodec[TestDouble]
  }

  case class Name(value: String) {
    override def toString: String = s"Name#$value"
  }
  object Name {
    def fromStr(value: String) = Name(value.split("#").last)
  }
  case class User(name: Name)
  case class UserStored(name: String) {
    def toAPI = User(Name.fromStr(name))
  }

  "encode/decode TestNoDouble" in check {
    Prop.forAllNoShrink {
      testData: TestNoDouble =>
        val encoded = DynamoEncoder[TestNoDouble].encode(testData)
        val decoded = DynamoDecoder[TestNoDouble].decode(encoded).toOption.get
        testData == decoded
    }
  }

  "encode/decode TestDouble" in check {
    Prop.forAllNoShrink {
      testData: TestDouble =>
        val encoded = DynamoEncoder[TestDouble].encode(testData)
        val decoded = DynamoDecoder[TestDouble].decode(encoded).toOption.get
        testData == decoded
    }
  }

  "new decoder test" in check {
    implicit val encoder: D4SEncoder[TestNoDouble] = D4SEncoder.derived[TestNoDouble]
    implicit val decoder: D4SDecoder[TestNoDouble] = D4SDecoder.derived[TestNoDouble]

    Prop.forAllNoShrink {
      testData: TestNoDouble =>
        val encoded = D4SEncoder[TestNoDouble].encode(testData)
        val decoded = D4SDecoder[TestNoDouble].decode(encoded).toOption.get
        testData == decoded
    }
  }

  "new decoder test: byte arrays" in check {
    implicit val encoder: D4SEncoder[TestByteArray] = D4SEncoder.derived[TestByteArray]
    implicit val decoder: D4SDecoder[TestByteArray] = D4SDecoder.derived[TestByteArray]

    Prop.forAllNoShrink {
      testData: TestByteArray =>
        val encoded = D4SEncoder[TestByteArray].encode(testData)
        val decoded = D4SDecoder[TestByteArray].decode(encoded).toOption.get
        testData.a.sameElements(decoded.a)
    }
  }

}
