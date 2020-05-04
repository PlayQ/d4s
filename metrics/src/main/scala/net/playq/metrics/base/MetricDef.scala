package net.playq.metrics.base
import izumi.fundamentals.platform.language.unused
import net.playq.metrics.base.LowPriorityInstances.{_Decoder, _Encoder}

import scala.util.Try
import scala.util.matching.Regex

sealed trait MetricDef extends Product with Serializable {
  def role: String
  def label: String
}

object MetricDef extends LowPriorityInstances {
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

private[metrics] sealed trait LowPriorityInstances {
  implicit def encoderFromCirce[R[_]](implicit @unused enc: _Encoder[R], F0: R[String]) = {
    val F = F0.asInstanceOf[io.circe.Encoder[String]]
    F.contramap[MetricDef](MetricDef.encode)
  }

  implicit def decoderFromCirce[R[_]](implicit @unused enc: _Decoder[R], F0: R[String]) = {
    val F = F0.asInstanceOf[io.circe.Decoder[String]]
    F.emap(MetricDef.decode(_).left.map(_.getMessage))
  }
}

object LowPriorityInstances {
  sealed abstract class _Encoder[R[_]]
  object _Encoder {
    @inline implicit final def fromCirce: _Encoder[io.circe.Encoder] = null
  }

  sealed abstract class _Decoder[R[_]]
  object _Decoder {
    @inline implicit final def fromCirce: _Encoder[io.circe.Decoder] = null
  }
}
