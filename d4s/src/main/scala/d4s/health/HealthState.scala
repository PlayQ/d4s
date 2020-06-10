package d4s.health

sealed trait HealthState extends Product with Serializable

object HealthState {
  def all: Seq[HealthState] = Seq(OK, DEFUNCT, UNKNOWN)
  def parse(value: String): HealthState = value match {
    case "OK"      => OK
    case "DEFUNCT" => DEFUNCT
    case "UNKNOWN" => UNKNOWN
  }

  case object OK extends HealthState { override def toString: String = "OK" }
  case object DEFUNCT extends HealthState { override def toString: String = "DEFUNCT" }
  case object UNKNOWN extends HealthState { override def toString: String = "UNKNOWN" }
}
