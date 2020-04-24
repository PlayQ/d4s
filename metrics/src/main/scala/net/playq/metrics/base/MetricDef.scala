package net.playq.metrics.base
import scala.util.Try
import scala.util.matching.Regex

sealed trait MetricDef extends Product with Serializable {
  def role: String
  def label: String
}

object MetricDef {
  final case class MetricCounter(role: String, label: String, initial: Int) extends MetricDef
  final case class MetricHistogram(role: String, label: String, initial: Double) extends MetricDef
  final case class MetricTimer(role: String, label: String, initial: Double) extends MetricDef
  final case class MetricMeter(role: String, label: String, initial: Double) extends MetricDef
  final case class MetricGauge(role: String, label: String, initial: Double) extends MetricDef

  def encode(metric: MetricDef): String = {
    metric match {
      case MetricCounter(role, label, initial)   => s"""{"counter":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricHistogram(role, label, initial) => s"""{"histogram":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricTimer(role, label, initial)     => s"""{"timer":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricMeter(role, label, initial)     => s"""{"meter":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricGauge(role, label, initial)     => s"""{"gauge":{"role":"$role","label":"$label","initial":$initial}}"""
    }
  }

  object MetricsRegex {
    private def regexForType(t: String): Regex = s"""\\{"$t":\\{"role":"(.*)","label":"(.*)","initial":(.*)}}""".r
    val counter: Regex                         = regexForType("counter")
    val histogram: Regex                       = regexForType("histogram")
    val timer: Regex                           = regexForType("timer")
    val meter: Regex                           = regexForType("meter")
    val gauge: Regex                           = regexForType("gauge")
  }

  def decode(metric: String): Either[Throwable, MetricDef] = {
    metric match {
      case MetricsRegex.counter(role, label, initial)   => Try(initial.toInt).map(MetricCounter(role, label, _)).toEither
      case MetricsRegex.histogram(role, label, initial) => Try(initial.toDouble).map(MetricHistogram(role, label, _)).toEither
      case MetricsRegex.timer(role, label, initial)     => Try(initial.toDouble).map(MetricTimer(role, label, _)).toEither
      case MetricsRegex.meter(role, label, initial)     => Try(initial.toDouble).map(MetricMeter(role, label, _)).toEither
      case MetricsRegex.gauge(role, label, initial)     => Try(initial.toDouble).map(MetricGauge(role, label, _)).toEither
      case other                                        => Left(new RuntimeException(s"Unexpected metric: $other. Can not parse."))
    }
  }

  def encodeAll(metrics: List[MetricDef]): String = metrics.map(encode).mkString("\n")
}
