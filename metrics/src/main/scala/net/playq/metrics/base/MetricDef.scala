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
  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have circe-core as a dependency without REQUIRING a circe-core dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    */
  implicit def encoderFromCirce[R[_]](implicit @unused enc: _Encoder[R]): R[MetricDef] = {
    def encodeMetricDef[A](role: String, label: String, initial: A) = {
      io.circe.Json.obj(
        "role"  -> io.circe.Json.fromString(role),
        "label" -> io.circe.Json.fromString(label),
        initial match {
          case i: Int    => "initial" -> io.circe.Json.fromInt(i)
          case d: Double => "initial" -> io.circe.Json.fromDoubleOrNull(d)
          case _         => "initial" -> io.circe.Json.Null
        },
      )
    }

    io.circe.Encoder.AsObject
      .instance[MetricDef]({
        case MetricDef.MetricCounter(role, label, initial)   => io.circe.JsonObject.fromMap(Map("counter" -> encodeMetricDef(role, label, initial)))
        case MetricDef.MetricHistogram(role, label, initial) => io.circe.JsonObject.fromMap(Map("histogram" -> encodeMetricDef(role, label, initial)))
        case MetricDef.MetricTimer(role, label, initial)     => io.circe.JsonObject.fromMap(Map("timer" -> encodeMetricDef(role, label, initial)))
        case MetricDef.MetricMeter(role, label, initial)     => io.circe.JsonObject.fromMap(Map("meter" -> encodeMetricDef(role, label, initial)))
        case MetricDef.MetricGauge(role, label, initial)     => io.circe.JsonObject.fromMap(Map("gauge" -> encodeMetricDef(role, label, initial)))
      }).asInstanceOf[R[MetricDef]]
  }

  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have circe-core as a dependency without REQUIRING a circe-core dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    */
  implicit def decoderFromCirce[R[_]](implicit @unused enc: _Decoder[R]): R[MetricDef] = {
    def parserC(c: io.circe.ACursor) = {
      for {
        role    <- c.downField("role").as[String]
        label   <- c.downField("label").as[String]
        initial <- c.downField("initial").as[Int]
      } yield (role, label, initial)
    }

    def parserD(c: io.circe.ACursor) = {
      for {
        role    <- c.downField("role").as[String]
        label   <- c.downField("label").as[String]
        initial <- c.downField("initial").as[Double]
      } yield (role, label, initial)
    }

    io.circe.Decoder
      .instance(cursor => {
        val maybeContent =
          cursor.keys
            .flatMap(_.headOption)
            .toRight(io.circe.DecodingFailure("No type name found in JSON, expected JSON of form { \"type_name\": { ...fields } }", cursor.history))
        for (fname <- maybeContent; value = cursor.downField(fname);
          result <- fname match {
            case "counter" =>
              parserC(value).map { case (r, l, i) => MetricDef.MetricCounter(r, l, i) }
            case "timer" =>
              parserD(value).map { case (r, l, i) => MetricDef.MetricTimer(r, l, i) }
            case "meter" =>
              parserD(value).map { case (r, l, i) => MetricDef.MetricMeter(r, l, i) }
            case "histogram" =>
              parserD(value).map { case (r, l, i) => MetricDef.MetricHistogram(r, l, i) }
            case "gauge" =>
              parserD(value).map { case (r, l, i) => MetricDef.MetricGauge(r, l, i) }
            case _ =>
              val cname = "net.playq.metrics.MetricDef"
              val alts  = List("counter", "timer", "meter", "histogram", "gauge").mkString(",")
              Left(io.circe.DecodingFailure(s"Can't decode type $fname as $cname, expected one of [$alts]", value.history))
          }) yield {
          result
        }
      }).asInstanceOf[R[MetricDef]]
  }
}

/**
  * This is done to disambiguate between `encoder` and `decoder` functions in
  * LowPriorityInstances sealed trait. Basically, these values are required to
  * give the compiler a hint about what function to use.
  */
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
