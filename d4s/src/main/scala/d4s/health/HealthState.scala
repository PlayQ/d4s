package d4s.health

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}

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

  implicit val encodeHealthState: Encoder[HealthState]       = Encoder.encodeString.contramap(_.toString)
  implicit val decodeHealthState: Decoder[HealthState]       = Decoder.decodeString.emapTry(v => Try(HealthState.parse(v)))
  implicit val encodeKeyHealthState: KeyEncoder[HealthState] = KeyEncoder.encodeKeyString.contramap(_.toString)
  implicit val decodeKeyHealthState: KeyDecoder[HealthState] = (key: String) => Try(HealthState.parse(key)).toOption
}
