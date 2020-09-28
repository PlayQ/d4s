package d4s.env

import d4s.test.envs.D4SDockerModule
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.effect.modules.ZIODIEffectModule
import izumi.distage.plugins.PluginDef
import logstage.LogBIO
import net.playq.aws.tagging.modules.AwsTagsModule
import net.playq.metrics.modules.DummyMetricsModule
import zio.{IO, Task}

object D4STestPlugin extends PluginDef {
  include(AwsTagsModule)
  include(DummyMetricsModule[IO])

  include(D4SDockerModule[IO])
  include(DockerSupportModule[Task])

  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
}
