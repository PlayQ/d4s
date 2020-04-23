package net.playq.metrics.base

import scala.util.Try

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

  private def encode(metric: MetricDef): String = {
    metric match {
      case MetricCounter(role, label, initial)   => s"""{"counter":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricHistogram(role, label, initial) => s"""{"histogram":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricTimer(role, label, initial)     => s"""{"timer":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricMeter(role, label, initial)     => s"""{"meter":{"role":"$role","label":"$label","initial":$initial}}"""
      case MetricGauge(role, label, initial)     => s"""{"gauge":{"role":"$role","label":"$label","initial":$initial}}"""
    }
  }

  def decode(metric: String): Either[Throwable, MetricDef] = {
    metric match {
      case s"""{"counter":{"role":"$role","label":"$label","initial":$initial}}"""   => Try(initial.toInt).map(MetricCounter(role, label, _)).toEither
      case s"""{"histogram":{"role":"$role","label":"$label","initial":$initial}}""" => Try(initial.toDouble).map(MetricHistogram(role, label, _)).toEither
      case s"""{"timer":{"role":"$role","label":"$label","initial":$initial}}"""     => Try(initial.toDouble).map(MetricTimer(role, label, _)).toEither
      case s"""{"meter":{"role":"$role","label":"$label","initial":$initial}}"""     => Try(initial.toDouble).map(MetricMeter(role, label, _)).toEither
      case s"""{"gauge":{"role":"$role","label":"$label","initial":$initial}}"""     => Try(initial.toDouble).map(MetricGauge(role, label, _)).toEither
      case other                                                                     => Left(new RuntimeException(s"Unexpected metric: $other. Can not parse."))
    }
  }

  def encodeAll(metrics: List[MetricDef]): String = metrics.map(encode).mkString("\n")
}
