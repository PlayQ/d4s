package d4s.test.envs

import d4s.DynamoDDLService
import d4s.config.DynamoConfig
import d4s.test.envs.DynamoTestEnv.DDLDown
import distage.{Activation, DIKey, ModuleDef}
import izumi.distage.docker.Docker
import izumi.distage.docker.modules.DockerContainerModule
import izumi.distage.model.definition.StandardAxis.Env
import izumi.distage.model.definition.{DIResource, Module}
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageBIOSpecScalatest
import logstage.LogBIO
import net.playq.aws.tagging.AwsNameSpace
import zio.{IO, Task}

trait DynamoTestEnv extends DistageBIOSpecScalatest[IO] {
  val envTest = Activation(Env -> Env.Test)

  override protected def config: TestConfig = super.config.copy(
    moduleOverrides  = moduleOverrides,
    activation       = super.config.activation ++ envTest,
    memoizationRoots = memoizationRoots,
    forcedRoots      = additionalRoots,
  )

  def moduleOverrides: Module = new ModuleDef {
    make[DDLDown]

    include(new DockerContainerModule[Task] overridenBy new ModuleDef {
      make[Docker.ClientConfig].fromValue(dockerConf)
    })
    include(D4SDockerModule[IO])
  }

  def memoizationRoots: Set[DIKey] = Set(
    DIKey.get[DynamoConfig],
    DIKey.get[AwsNameSpace],
    DIKey.get[DynamoDDLService[IO]],
    DIKey.get[DDLDown],
  )

  def additionalRoots: Set[DIKey] = Set(
    DIKey.get[DynamoDDLService[IO]],
    DIKey.get[DDLDown],
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
  final case class DDLDown(
    dynamoDDLService: DynamoDDLService[IO],
    logger: LogBIO[IO]
  ) extends DIResource.Self[IO[Throwable, ?], DDLDown] {
    override def acquire: IO[Throwable, Unit] = IO.unit
    override def release: IO[Throwable, Unit] = {
      for {
        _ <- logger.info("Deleting all tables")
        _ <- dynamoDDLService.down()
      } yield ()
    }
  }
}
