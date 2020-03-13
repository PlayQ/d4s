package net.playq.metrics

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.MILLIS
import java.util.concurrent.TimeUnit.MILLISECONDS

import izumi.functional.bio.{BIOApplicative, BIOMonad, Clock2, F}

import scala.concurrent.duration.FiniteDuration

trait Metrics[F[_, _]] {
  def inc(label: String, value: Int   = 1)(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[Nothing, Unit]
  def dec(label: String, value: Int   = 1)(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[Nothing, Unit]
  def mark(label: String, value: Long = 1L)(implicit macroSaveMeterMetric: MacroMetricMeter[label.type]): F[Nothing, Unit]
  def record(label: String, value: Long)(implicit macroSaveHistogramMetric: MacroMetricHistogram[label.type]): F[Nothing, Unit]
  def timerUpdate(label: String, value: FiniteDuration)(implicit macroSaveTimerMetric: MacroMetricTimer[label.type]): F[Nothing, Unit]
  def setGauge(label: String, effect: () => Long)(implicit macroSaveGaugeMetric: MacroMetricGauge[label.type]): F[Nothing, Unit]

  def withTimer[E, T](label: String)(t: => F[E, T])(implicit macroSaveTimerMetric: MacroMetricTimer[label.type]): F[E, T]
  def withMark[E, T](label: String, value: Long = 1L)(t: => F[E, T])(implicit macroSaveMeterMetric: MacroMetricMeter[label.type]): F[E, T]
  def withInc[E, T](label: String, value: Int   = 1)(t: => F[E, T])(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[E, T]
  def withDec[E, T](label: String, value: Int   = 1)(t: => F[E, T])(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[E, T]
}

object Metrics {
  def apply[F[_, _]: Metrics]: Metrics[F] = implicitly

  final class Empty[F[+_, +_]: BIOApplicative] extends Metrics[F] {
    override def inc(label: String, value: Int)(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[Nothing, Unit] = F.unit
    override def dec(label: String, value: Int)(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[Nothing, Unit] = F.unit
    override def mark(label: String, value: Long = 0)(implicit macroSaveMeterMetric: MacroMetricMeter[label.type]): F[Nothing, Unit] = F.unit
    override def record(label: String, value: Long)(implicit macroSaveHistogramMetric: MacroMetricHistogram[label.type]): F[Nothing, Unit]        = F.unit
    override def timerUpdate(label: String, value: FiniteDuration)(implicit macroSaveTimerMetric: MacroMetricTimer[label.type]): F[Nothing, Unit] = F.unit
    override def setGauge(label: String, effect: () => Long)(implicit macroSaveCounterMetric: MacroMetricGauge[label.type]): F[Nothing, Unit]     = F.unit

    override def withTimer[E, T](label: String)(t: => F[E, T])(implicit macroSaveTimerMetric: MacroMetricTimer[label.type]): F[E, T]               = t
    override def withMark[E, T](label: String, value: Long)(t: => F[E, T])(implicit macroSaveMeterMetric: MacroMetricMeter[label.type]): F[E, T]   = t
    override def withInc[E, T](label: String, value: Int)(t: => F[E, T])(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[E, T] = t
    override def withDec[E, T](label: String, value: Int)(t: => F[E, T])(implicit macroSaveCounterMetric: MacroMetricCounter[label.type]): F[E, T] = t
  }

  implicit final class TimerUpdateDiff[F[+_, +_]](private val metrics: Metrics[F]) extends AnyVal {
    def timerUpdateDiff(metric: String, start: ZonedDateTime, end: ZonedDateTime)(implicit macroSaveTimerMetric: MacroMetricTimer[metric.type]): F[Nothing, Unit] = {
      val duration = FiniteDuration(MILLIS.between(start, end), MILLISECONDS)
      metrics.timerUpdate(metric, duration)
    }

    def timerUpdateDiffNow(metric: String, start: ZonedDateTime)(implicit
                                                                 macroSaveTimerMetric: MacroMetricTimer[metric.type],
                                                                 clock: Clock2[F],
                                                                 F: BIOMonad[F]): F[Nothing, Unit] = {
      for {
        end      <- clock.now()
        duration = FiniteDuration(MILLIS.between(start, end), MILLISECONDS)
        _        <- metrics.timerUpdate(metric, duration)
      } yield ()
    }
  }
}
