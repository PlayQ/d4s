package net.playq

import net.playq.metrics.macrodefs.MacroMetricBase

package object metrics {
  final type MacroMetricTimer[S <: String]     = MacroMetricBase.Timer#MetricBase[S, ?]
  final type MacroMetricMeter[S <: String]     = MacroMetricBase.Meter#MetricBase[S, ?]
  final type MacroMetricCounter[S <: String]   = MacroMetricBase.Counter#MetricBase[S, ?]
  final type MacroMetricGauge[S <: String]     = MacroMetricBase.Gauge#MetricBase[S, ?]
  final type MacroMetricHistogram[S <: String] = MacroMetricBase.Histogram#MetricBase[S, ?]
}
