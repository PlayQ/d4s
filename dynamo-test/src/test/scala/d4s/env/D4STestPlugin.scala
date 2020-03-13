package d4s.env

import d4s.modules.D4SModule
import izumi.distage.effect.modules.ZIODIEffectModule
import izumi.distage.plugins.PluginDef
import logstage.LogBIO
import net.playq.aws.tagging.modules.AwsTagsModule
import net.playq.metrics.modules.DummyMetricsModule
import zio.IO

object D4STestPlugin extends PluginDef {
  include(ZIODIEffectModule)
  include(AwsTagsModule)
  include(D4SModule[IO])
  include(DummyMetricsModule[IO])
  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
}
