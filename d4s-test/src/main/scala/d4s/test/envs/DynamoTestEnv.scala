package d4s.test.envs

import d4s.DynamoDDLService
import d4s.config.DynamoConfig
import d4s.test.envs.DynamoTestEnv.DDLDown
import distage.{DIKey, ModuleDef}
import izumi.distage.docker.Docker
import izumi.distage.docker.modules.DockerContainerModule
import izumi.distage.model.definition.DIResource
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageBIOSpecScalatest
import izumi.functional.bio.BIO
import logstage.LogBIO
import net.playq.aws.tagging.AwsNameSpace

trait DynamoTestEnv[F[+_, +_]] extends DistageBIOSpecScalatest[F] {
  override protected def config: TestConfig = super.config.copy(
    moduleOverrides = new ModuleDef {
      make[DDLDown[F]]
      include(new DockerContainerModule[F[Throwable, ?]] overridenBy new ModuleDef {
        make[Docker.ClientConfig].fromValue(dockerConf)
      })
      include(D4SDockerModule[F])
    },
    memoizationRoots = Set(
      DIKey.get[DynamoConfig],
      DIKey.get[AwsNameSpace],
      DIKey.get[DynamoDDLService[F]],
      DIKey.get[DDLDown[F]],
    ),
    forcedRoots = Set(
      DIKey.get[DynamoDDLService[F]],
      DIKey.get[DDLDown[F]],
    ),
  )

  def dockerConf: Docker.ClientConfig = Docker.ClientConfig(
    readTimeoutMs    = 8000,
    connectTimeoutMs = 3000,
    allowReuse       = true,
    useRemote        = false,
    useRegistry      = false,
    remote           = None,
    registry         = None,
  )
}

object DynamoTestEnv {
  final case class DDLDown[F[+_, +_]: BIO](
    dynamoDDLService: DynamoDDLService[F],
    logger: LogBIO[F]
  ) extends DIResource.Self[F[Throwable, ?], DDLDown[F]] {
    override def acquire: F[Throwable, Unit] = BIO[F].unit
    override def release: F[Throwable, Unit] = {
      for {
        _ <- logger.info("Deleting all tables")
        _ <- dynamoDDLService.down()
      } yield ()
    }
  }
}
