package d4s.codecs

import d4s.codecs.Fixtures._
import d4s.codecs.circe.{D4SCirceAttributeCodec, D4SCirceCodec}
import d4s.models.DynamoException.DecoderException
import io.circe.generic.extras.semiauto
import io.circe.{Decoder, Encoder}
import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

final class DynamoCodecTest extends AnyWordSpec with Checkers {

  "encode options" in {
    case class TestNullable(str: Option[String])
    val encoder  = D4SEncoder.derived[TestNullable]
    val encoderWithoutNulls    = D4SEncoder.WithoutNulls.derived[TestNullable]

    val encoded = encoder.encodeObject(TestNullable(None))
    val encoded2 = encoder.encodeObject(TestNullable(Some("test")))
    val encoded3 = encoderWithoutNulls.encodeObject(TestNullable(None))
    val encoded4 = encoderWithoutNulls.encodeObject(TestNullable(Some("test")))

    assert(encoded.nonEmpty)
    assert(encoded3.isEmpty)
    assert(encoded2.get("str").contains(AttributeValue.builder().s("test").build()))
    assert(encoded2.equals(encoded4))
  }

  "decode options from missed attribute" in {
    val decoder = D4SDecoder.optionDecoder[Int]
    assert(decoder.decodeOptional(None, "").exists(_.isEmpty))
  }

  "encode/decode TestCaseClass" in check {
    Prop.forAllNoShrink {
      testData: TestCaseClass =>
        val circeCodec    = D4SCirceCodec.derived[TestCaseClass]
        val magnoliaCodec = D4SCodec.derived[TestCaseClass]

        val encoded = circeCodec.encodeObject(testData)
        val decoded = circeCodec.decodeObject(encoded).toOption.get

        val magnoliaEncoded = magnoliaCodec.encodeObject(testData)
        val magnoliaDecoded = magnoliaCodec.decodeObject(magnoliaEncoded).toOption.get
        testData == decoded && decoded == magnoliaDecoded && encoded == magnoliaEncoded
    }
  }

  "encode/decode TestDouble" in check {
    Prop.forAllNoShrink {
      testData: TestDouble =>
        val circeCodec    = D4SCirceCodec.derived[TestDouble]
        val magnoliaCodec = D4SCodec.derived[TestDouble]

        val encoded = circeCodec.encodeObject(testData)
        val decoded = circeCodec.decodeObject(encoded).toOption.get

        val magnoliaEncoded = magnoliaCodec.encodeObject(testData)
        val magnoliaDecoded = magnoliaCodec.decodeObject(magnoliaEncoded).toOption.get
        testData == decoded && decoded == magnoliaDecoded && encoded == magnoliaEncoded
    }
  }

  "new decoder test: byte arrays" in check {
    implicit val encoder: D4SEncoder[TestByteArray] = D4SEncoder.derived[TestByteArray]
    implicit val decoder: D4SDecoder[TestByteArray] = D4SDecoder.derived[TestByteArray]

    Prop.forAllNoShrink {
      testData: TestByteArray =>
        val encoded = D4SEncoder.encodeObject(testData)
        val decoded = D4SDecoder.decodeObject[TestByteArray](encoded).toOption.get

        assert(encoded.values.exists(_.b().asByteArray() sameElements testData.a))
        testData.a.sameElements(decoded.a)
    }
  }

  "case object derivation" in {
    val testCodec = D4SAttributeCodec.derived[Color]
    val codec = {
      implicit val enc: Encoder[Color] = semiauto.deriveEnumerationEncoder[Color]
      implicit val dec: Decoder[Color] = semiauto.deriveEnumerationDecoder[Color]
      D4SCirceAttributeCodec.derived[Color]
    }

    val encoded = codec.encode(Red)
    assert(encoded == testCodec.encode(Red))
    assert(codec.decode(encoded) == testCodec.decode(encoded))
  }

  "sealed trait test" in check {
    Prop.forAllNoShrink {
      v: Either[String, Int] =>
        val codec1: D4SCodec[Either[String, Int]]                  = D4SCodec.derived
        val codec2: D4SAttributeCodec[Either[String, Int]]         = D4SAttributeCodec.derived
        val result1: Either[DecoderException, Either[String, Int]] = codec1.decode(codec1.encode(v))
        val result2: Either[DecoderException, Either[String, Int]] = codec2.decode(codec2.encode(v))

        assert(result1 == result2)
        assert(result1 == Right(v))
        true
    }
  }

  "sealed trait test #2" in check {
    Prop.forAllNoShrink {
      v: AmbiguousResult =>
        val codec: D4SCodec[AmbiguousResult] = D4SCodec.derived[AmbiguousResult]
        val result                           = codec.decodeObject(codec.encodeObject(v))
        assert(result == Right(v))
        result == Right(v)
    }
  }

  "custom sealed trait test" in check {
    Prop.forAllNoShrink {
      v: AmbiguousResult =>
        val codec: D4SCodec[AmbiguousResult] = {
          implicit val customEitherCodec: D4SCodec[Either[String, Int]] = D4SCodec.fromPair(
            D4SEncoder.traitEncoder[Either[String, Int]] {
              case Left(_)  => "l" -> D4SEncoder.derived[Left[String, Int]]
              case Right(_) => "r" -> D4SEncoder.derived[Right[String, Int]]
            },
            D4SDecoder.traitDecoder[Either[String, Int]]("Either[String, Int]") {
              Map(
                "l" -> D4SDecoder.derived[Left[String, Int]],
                "r" -> D4SDecoder.derived[Right[String, Int]],
              )
            },
          )
          val _ = customEitherCodec
          D4SCodec.derived
        }
        val map       = codec.encodeObject(v)
        val result    = codec.decodeObject(map)
        val qualifier = map("v").m().asScala.head._1
        assert(qualifier == "r" || qualifier == "l")
        assert(result == Right(v))
        (qualifier == "r" || qualifier == "l"
        || result == Right(v))
    }
  }

  "binary set test" in check {
    Prop.forAllNoShrink {
      testData: TestBinarySet =>
        val encoded = D4SEncoder.encodeObject(testData)
        val decoded = D4SDecoder.decodeObject[TestBinarySet](encoded).toOption.get

        val sdkBytesSet = testData.a.map(SdkBytes.fromByteArray)

        encoded.values.head.bs().asScala.toSet == sdkBytesSet &&
        decoded.a.map(SdkBytes.fromByteArray) == sdkBytesSet
    }
  }

  "string set test" in check {
    Prop.forAllNoShrink {
      testData: TestStringSet =>
        val encoded = D4SEncoder.encodeObject(testData)
        val decoded = D4SDecoder.decodeObject[TestStringSet](encoded).toOption.get

        encoded.values.head.ss().asScala.toSet == testData.a &&
        decoded.a == testData.a
    }
  }

}
