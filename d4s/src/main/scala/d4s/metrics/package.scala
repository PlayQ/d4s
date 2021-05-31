package d4s

package object metrics {
  final type MacroMetricDynamoMeter[S <: String] = MacroMetricsDynamo.Meter.MetricBase[S, ?]
  final type MacroMetricDynamoTimer[S <: String] = MacroMetricsDynamo.Timer.MetricBase[S, ?]
}
