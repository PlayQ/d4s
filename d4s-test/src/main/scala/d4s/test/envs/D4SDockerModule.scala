package d4s.test.envs

import d4s.config.DynamoConfig
import d4s.modules.D4SModule
import distage.{ModuleDef, TagKK}
import izumi.distage.config.ConfigModuleDef
import izumi.distage.docker.bundled.{DynamoDocker, DynamoDockerModule}
import izumi.distage.model.definition.Id

class D4SDockerModule[F[+_, +_]: TagKK] extends ConfigModuleDef {
  include(D4SDockerModule.config)
  include(DynamoDockerModule[F[Throwable, ?]])
  include(D4SModule[F] overridenBy new ModuleDef {
    make[DynamoConfig].from {
      (cfg: DynamoConfig @Id("test-config"), docker: DynamoDocker.Container) =>
        val knownAddress = docker.availablePorts.first(DynamoDocker.primaryPort)
        cfg.copy(endpointUrl = Some(s"http://${knownAddress.hostV4}:${knownAddress.port}"))
    }
  })
}

object D4SDockerModule {
  def apply[F[+_, +_]: TagKK] = new D4SDockerModule[F]

  val config: ConfigModuleDef = new ConfigModuleDef {
    makeConfig[DynamoConfig]("aws.dynamo").named("test-config")
  }
}
