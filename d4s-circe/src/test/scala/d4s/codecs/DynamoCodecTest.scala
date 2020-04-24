package d4s.codecs

import d4s.codecs.Fixtures._
import d4s.codecs.circe.{D4SCirceAttributeCodec, D4SCirceCodec}
import d4s.models.DynamoException.DecoderException
import io.circe.generic.extras.semiauto
import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

@SuppressWarnings(Array("EitherGet", "FinalModifierOnCaseClass"))
class DynamoCodecTest extends AnyWordSpec with Checkers {
  "encode/decode TestCaseClass" in check {
    Prop.forAllNoShrink {
      testData: TestCaseClass =>
        val circeCodec    = D4SCirceCodec.derived[TestCaseClass]
        val magnoliaCodec = D4SCodec.derived[TestCaseClass]

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
        val circeCodec    = D4SCirceCodec.derived[TestDouble]
        val magnoliaCodec = D4SCodec.derived[TestDouble]

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

  "case object derivation" in {
    val testCodec    = D4SAttributeCodec.derived[Color]
    implicit val enc = semiauto.deriveEnumerationEncoder[Color]
    implicit val dec = semiauto.deriveEnumerationDecoder[Color]
    val codec0       = D4SCirceAttributeCodec.derived[Color]

    val encoded = codec0.encodeAttribute(Red)
    assert(encoded == testCodec.encodeAttribute(Red))
    assert(codec0.decodeAttribute(encoded) == testCodec.decodeAttribute(encoded))
  }

  "sealed trait test" in check {
    Prop.forAllNoShrink {
      v: Either[String, Int] =>
        val codec: D4SCodec[Either[String, Int]]                  = D4SCodec.derived
        val result: Either[DecoderException, Either[String, Int]] = codec.decodeAttribute(codec.encodeAttribute(v))
        assert(result == Right(v))
        result == Right(v)
    }
  }

  "sealed trait test #2" in check {
    Prop.forAllNoShrink {
      v: AmbiguousResult =>
        val codec: D4SCodec[AmbiguousResult] = D4SCodec.derived[AmbiguousResult]
        val result                           = codec.decode(codec.encode(v))
        assert(result == Right(v))
        result == Right(v)
    }
  }

}
