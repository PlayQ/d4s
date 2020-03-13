package net.playq

import net.playq.metrics.macrodefs.MacroMetricBase

package object metrics {
  final type MacroMetricTimer[S <: String]     = MacroMetricBase.Timer#MetricBase[S, _]
  final type MacroMetricMeter[S <: String]     = MacroMetricBase.Meter#MetricBase[S, _]
  final type MacroMetricCounter[S <: String]   = MacroMetricBase.Counter#MetricBase[S, _]
  final type MacroMetricGauge[S <: String]     = MacroMetricBase.Gauge#MetricBase[S, _]
  final type MacroMetricHistogram[S <: String] = MacroMetricBase.Histogram#MetricBase[S, _]
}
