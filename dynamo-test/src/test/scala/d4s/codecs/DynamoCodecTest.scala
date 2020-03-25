package d4s.codecs

import d4s.codecs.circe.D4SCirceCodec
import d4s.env.DynamoRnd
import io.circe.{Codec, derivation}
import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

@SuppressWarnings(Array("EitherGet", "FinalModifierOnCaseClass"))
class DynamoCodecTest extends AnyWordSpec with Checkers with DynamoRnd {

  case class TestCaseClass(
    a: Int,
    b: Long,
    c: Boolean,
    d: String,
    e: Seq[Int],
    f: Option[Unit],
    m: Map[String, Int],
    wrap: TestDataWrap
  )
  object TestCaseClass {
    implicit val circeCodec: Codec.AsObject[TestCaseClass] = derivation.deriveCodec[TestCaseClass]
  }

  case class TestDataWrap(data: String)
  object TestDataWrap {
    implicit val circeCodec: Codec.AsObject[TestDataWrap] = derivation.deriveCodec[TestDataWrap]
    implicit val magnoliaCodec: D4SCodec[TestDataWrap] = D4SCodec.derive[TestDataWrap]
  }

  case class TestByteArray(a: Array[Byte])

  case class TestDouble(a: Double)
  object TestDouble {
    implicit val circeCodec: Codec.AsObject[TestDouble] = derivation.deriveCodec[TestDouble]
  }

  "encode/decode TestCaseClass" in check {
    Prop.forAllNoShrink {
      testData: TestCaseClass =>
        val circeCodec    = D4SCirceCodec.derive[TestCaseClass]
        val magnoliaCodec = D4SCodec.derive[TestCaseClass]

        val encoded = circeCodec.encode(testData)
        val decoded = circeCodec.decode(encoded).toOption.get

        val magnoliaEncoded = magnoliaCodec.encode(testData)
        val magnoliaDecoded = magnoliaCodec.decode(magnoliaEncoded).toOption.get
        testData == decoded && decoded == magnoliaDecoded && encoded == magnoliaEncoded
    }
  }

  "encode/decode TestDouble" in check {
    Prop.forAllNoShrink {
      testData: TestDouble =>
        val circeCodec    = D4SCirceCodec.derive[TestDouble]
        val magnoliaCodec = D4SCodec.derive[TestDouble]

        val encoded = circeCodec.encode(testData)
        val decoded = circeCodec.decode(encoded).toOption.get

        val magnoliaEncoded = magnoliaCodec.encode(testData)
        val magnoliaDecoded = magnoliaCodec.decode(magnoliaEncoded).toOption.get
        testData == decoded && decoded == magnoliaDecoded && encoded == magnoliaEncoded
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
