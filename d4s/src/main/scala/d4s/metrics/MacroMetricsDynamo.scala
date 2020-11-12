package d4s.metrics

import net.playq.metrics.macrodefs.MacroMetricBase

import scala.language.experimental.macros

object MacroMetricsDynamo {
  object Meter extends MacroMetricBase.Meter {
    override def createLabel(label: String): String = s"dynamo/request-error/$label"
    implicit def matMeter[S <: String]: MetricBase[S, MacroMetricsDynamo.discarded.type] = macro CompileTime.materializeImpl[S, MacroMetricsDynamo.discarded.type]
    override object CompileTime extends CompileTime
  }

  object Timer extends MacroMetricBase.Timer {
    override def createLabel(label: String): String = s"dynamo/$label/timer"
    implicit def matTimer[S <: String]: MetricBase[S, MacroMetricsDynamo.discarded.type] = macro CompileTime.materializeImpl[S, MacroMetricsDynamo.discarded.type]
    override object CompileTime extends CompileTime
  }

  object discarded {
    implicit def matMeter[S <: String]: MacroMetricDynamoMeter[S] = Meter.empty[S]
    implicit def matTimer[S <: String]: MacroMetricDynamoTimer[S] = Timer.empty[S]
  }
}
