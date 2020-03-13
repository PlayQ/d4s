package net.playq.metrics.modules

import distage.TagKK
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.StandardAxis.Env
import izumi.fundamentals.platform.functional.Identity2
import net.playq.metrics.Metrics

class DummyMetricsModule[F[+_, +_]: TagKK] extends ModuleDef {
  tag(Env.Test)

  make[Metrics[F]].from[Metrics.Empty[F]]
  make[Metrics[Identity2]].from[Metrics.Empty[Identity2]]
}

object DummyMetricsModule {
  def apply[F[+_, +_]: TagKK]: DummyMetricsModule[F] = new DummyMetricsModule[F]
}
