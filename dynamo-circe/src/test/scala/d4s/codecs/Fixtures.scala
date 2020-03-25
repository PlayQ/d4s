package d4s.codecs

import io.circe.{Codec, derivation}
import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary

object Fixtures {
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
    implicit lazy val arbitrary: Arbitrary[TestCaseClass] = MkArbitrary[TestCaseClass].arbitrary
  }

  case class TestDataWrap(data: String)
  object TestDataWrap {
    implicit val circeCodec: Codec.AsObject[TestDataWrap] = derivation.deriveCodec[TestDataWrap]
    implicit val magnoliaCodec: D4SCodec[TestDataWrap] = D4SCodec.derive[TestDataWrap]
    implicit lazy val arbitrary: Arbitrary[TestDataWrap] = MkArbitrary[TestDataWrap].arbitrary
  }

  case class TestByteArray(a: Array[Byte])
  object TestByteArray {
    implicit lazy val arbitrary: Arbitrary[TestByteArray] = MkArbitrary[TestByteArray].arbitrary
  }

  case class TestDouble(a: Double)
  object TestDouble {
    implicit val circeCodec: Codec.AsObject[TestDouble] = derivation.deriveCodec[TestDouble]
    implicit lazy val arbitrary: Arbitrary[TestDouble] = MkArbitrary[TestDouble].arbitrary
  }

}
