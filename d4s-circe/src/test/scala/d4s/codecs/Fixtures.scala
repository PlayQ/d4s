package d4s.codecs

import io.circe.{Codec, derivation}
import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary

object Fixtures {

  sealed trait Color
  case object Red extends Color
  case object Green extends Color
  case object Blue extends Color

  case class AmbiguousResult(v: Either[String, Int])
  object AmbiguousResult {
    implicit lazy val arbitrary: Arbitrary[AmbiguousResult] = MkArbitrary[AmbiguousResult].arbitrary
  }

  case class TestCaseClass(
    a: Int,
    b: Long,
    c: Boolean,
    d: String,
    e: Seq[Int],
    f: Option[Unit],
    m: Map[String, Int],
    wrap: TestDataWrap,
  )
  object TestCaseClass {
    implicit val circeCodec: Codec.AsObject[TestCaseClass] = derivation.deriveCodec[TestCaseClass]
    implicit lazy val arbitrary: Arbitrary[TestCaseClass]  = MkArbitrary[TestCaseClass].arbitrary
  }

  case class TestDataWrap(data: String)
  object TestDataWrap {
    implicit val circeCodec: Codec.AsObject[TestDataWrap] = derivation.deriveCodec[TestDataWrap]
    implicit val magnoliaCodec: D4SCodec[TestDataWrap]    = D4SCodec.derived[TestDataWrap]
    implicit lazy val arbitrary: Arbitrary[TestDataWrap]  = MkArbitrary[TestDataWrap].arbitrary
  }

  case class TestByteArray(a: Array[Byte])
  object TestByteArray {
    implicit lazy val arbitrary: Arbitrary[TestByteArray] = MkArbitrary[TestByteArray].arbitrary
  }

  case class TestDouble(a: Double)
  object TestDouble {
    implicit val circeCodec: Codec.AsObject[TestDouble] = derivation.deriveCodec[TestDouble]
    implicit lazy val arbitrary: Arbitrary[TestDouble]  = MkArbitrary[TestDouble].arbitrary
  }

  case class TestBinarySet(a: Set[Array[Byte]])
  object TestBinarySet extends WithD4S[TestBinarySet] {
    implicit lazy val arbitrary: Arbitrary[TestBinarySet] = MkArbitrary[TestBinarySet].arbitrary
  }

  case class TestStringSet(a: Set[String])
  object TestStringSet extends WithD4S[TestStringSet] {
    implicit lazy val arbitrary: Arbitrary[TestStringSet] = MkArbitrary[TestStringSet].arbitrary
  }

}
