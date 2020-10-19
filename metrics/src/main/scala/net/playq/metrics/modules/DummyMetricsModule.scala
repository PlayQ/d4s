package net.playq.metrics.modules

import distage.TagKK
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.StandardAxis.Mode
import izumi.fundamentals.platform.functional.Identity2
import net.playq.metrics.{Metrics, MetricsExtractor}

class DummyMetricsModule[F[+_, +_]: TagKK] extends ModuleDef {
  tag(Mode.Test)

  make[Metrics[F]].from[Metrics.Empty[F]]
  make[Metrics[Identity2]].from[Metrics.Empty[Identity2]]
  make[MetricsExtractor]
}

object DummyMetricsModule {
  def apply[F[+_, +_]: TagKK]: DummyMetricsModule[F] = new DummyMetricsModule[F]
}
