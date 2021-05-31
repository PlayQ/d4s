package net.playq.metrics.macrodefs

import net.playq.metrics.base.MetricDef
import net.playq.metrics.base.MetricDef.{MetricCounter, MetricGauge, MetricHistogram, MetricMeter, MetricTimer}
import net.playq.metrics.macrodefs.MacroMetricSaver.writeToFile
import net.playq.metrics._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

sealed trait MacroMetricBase {
  self =>

  def createMetrics(role: String, label: String): List[MetricDef]
  def createLabel(label: String): String = label

  @implicitNotFound("import ${DiscardType}.discarded._ to disable metric recording for ${S} - a metric created like this will not be visible in MetricsApi")
  trait MetricBase[S <: String, DiscardType] {
    def get: Option[String]
  }

  def empty[S <: String]: MetricBase[S, ?] = new MetricBase[S, Nothing] {
    override val get: Option[String] = None
  }

  val CompileTime: CompileTime
  abstract class CompileTime protected[this] {
    def materializeImpl[S <: String: c.WeakTypeTag, DiscardType: c.WeakTypeTag](
      c: blackbox.Context { type PrefixType = self.type }
    ): c.Expr[c.prefix.value.MetricBase[S, DiscardType]] = {
      import c.universe._
      val selfType    = c.prefix.actualType.typeSymbol.fullName
      val discardType = weakTypeOf[DiscardType].typeSymbol.fullName
      val label       = MacroMetricSaver.getConstantType[S](c, selfType, discardType)
      val labelExpr   = c.Expr[String](q"${createLabel(label)}")

      MacroMetricSaver.getRoles(c).foreach {
        role =>
          writeToFile(c, createMetrics(role, label))
      }

      reify[c.prefix.value.MetricBase[S, DiscardType]] {
        val prefix = c.prefix.splice
        new prefix.MetricBase[S, DiscardType] {
          override val get: Option[String] = Some(labelExpr.splice)
        }
      }
    }

  }
}

object MacroMetricBase {

  trait Timer extends MacroMetricBase {
    override def createMetrics(role: String, label: String): List[MetricDef] = List(MetricTimer(role, createLabel(label), 0.0))
  }
  object Timer extends Timer {
    implicit def materialize[S <: String]: MetricBase[S, discarded.type] = macro CompileTime.materializeImpl[S, discarded.type]
    override object CompileTime extends CompileTime
  }

  trait Counter extends MacroMetricBase {
    override def createMetrics(role: String, label: String): List[MetricDef] = List(MetricCounter(role, createLabel(label), 0))
  }
  object Counter extends Counter {
    implicit def materialize[S <: String]: MetricBase[S, discarded.type] = macro CompileTime.materializeImpl[S, discarded.type]
    override object CompileTime extends CompileTime
  }

  trait Meter extends MacroMetricBase {
    override def createMetrics(role: String, label: String): List[MetricDef] = List(MetricMeter(role, createLabel(label), 0.0))
  }
  object Meter extends Meter {
    implicit def materialize[S <: String]: MetricBase[S, discarded.type] = macro CompileTime.materializeImpl[S, discarded.type]
    override object CompileTime extends CompileTime
  }

  trait Gauge extends MacroMetricBase {
    override def createMetrics(role: String, label: String): List[MetricDef] = List(MetricGauge(role, createLabel(label), 0.0))
  }
  object Gauge extends Gauge {
    implicit def materialize[S <: String]: MetricBase[S, discarded.type] = macro CompileTime.materializeImpl[S, discarded.type]
    override object CompileTime extends CompileTime
  }

  trait Histogram extends MacroMetricBase {
    override def createMetrics(role: String, label: String): List[MetricDef] = List(MetricHistogram(role, createLabel(label), 0.0))
  }
  object Histogram extends Histogram {
    implicit def materialize[S <: String]: MetricBase[S, discarded.type] = macro CompileTime.materializeImpl[S, discarded.type]
    override object CompileTime extends CompileTime
  }

  object discarded {
    implicit def matCounter[S <: String]: MacroMetricCounter[S]     = Counter.empty[S]
    implicit def matTimer[S <: String]: MacroMetricTimer[S]         = Timer.empty[S]
    implicit def matMeter[S <: String]: MacroMetricMeter[S]         = Meter.empty[S]
    implicit def matGauge[S <: String]: MacroMetricGauge[S]         = Gauge.empty[S]
    implicit def matHistogram[S <: String]: MacroMetricHistogram[S] = Histogram.empty[S]
  }

}
