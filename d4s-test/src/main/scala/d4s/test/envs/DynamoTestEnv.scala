package d4s.test.envs

import d4s.DynamoDDLService
import d4s.config.DynamoConfig
import d4s.test.envs.DynamoTestEnv.DDLDown
import distage.{DIKey, ModuleDef, TagKK}
import izumi.distage.docker.Docker
import izumi.distage.docker.modules.DockerContainerModule
import izumi.distage.model.definition.DIResource
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.services.scalatest.dstest.DistageAbstractScalatestSpec
import izumi.functional.bio.{BIOApplicative, F}
import logstage.LogBIO
import net.playq.aws.tagging.AwsNameSpace

trait DynamoTestEnv[F[+_, +_]] extends DistageAbstractScalatestSpec[F[Throwable, ?]] {
  implicit def tagBIO: TagKK[F]

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
    readTimeoutMs    = 30000,
    connectTimeoutMs = 30000,
    allowReuse       = true,
    useRemote        = false,
    useRegistry      = false,
    remote           = None,
    registry         = None,
  )
}

object DynamoTestEnv {
  final case class DDLDown[F[+_, +_]: BIOApplicative](
    dynamoDDLService: DynamoDDLService[F],
    logger: LogBIO[F],
  ) extends DIResource.Self[F[Throwable, ?], DDLDown[F]] {
    override def acquire: F[Throwable, Unit] = F.unit
    override def release: F[Throwable, Unit] = {
      logger.info("Deleting all tables") *>
      dynamoDDLService.down()
    }
  }
}
