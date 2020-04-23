package d4s.health

import cats.syntax.either._
import d4s.codecs.CodecsUtils.CannotDecodeKeyValue
import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, D4SKeyDecoder, D4SKeyEncoder}

import scala.util._

sealed trait HealthState extends Product with Serializable

object HealthState {
  def all: Seq[HealthState] = Seq(OK, DEFUNCT, UNKNOWN)
  def parse(value: String): HealthState = value match {
    case "OK"      => OK
    case "DEFUNCT" => DEFUNCT
    case "UNKNOWN" => UNKNOWN
  }

  case object OK extends HealthState { override def toString: String      = "OK" }
  case object DEFUNCT extends HealthState { override def toString: String = "DEFUNCT" }
  case object UNKNOWN extends HealthState { override def toString: String = "UNKNOWN" }

  implicit val healthStateEncoder: D4SAttributeEncoder[HealthState] = D4SAttributeEncoder.derived[HealthState]
  implicit val healthStateDecoder: D4SDecoder[HealthState]          = D4SDecoder.derived[HealthState]
  implicit val encodeKeyHealthState: D4SKeyEncoder[HealthState]     = _.toString
  implicit val decodeKeyHealthState: D4SKeyDecoder[HealthState] = (key: String) =>
    Either.fromTry(Try(HealthState.parse(key))).leftMap(err => new CannotDecodeKeyValue(s"Cannot decode key $key as HealthState", Some(err)))
}
