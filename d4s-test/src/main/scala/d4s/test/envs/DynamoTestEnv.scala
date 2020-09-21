package d4s.test.envs

import d4s.DynamoDDLService
import d4s.test.envs.DynamoTestEnv.DDLDown
import distage.{DIKey, ModuleDef, TagKK}
import izumi.distage.model.definition.DIResource
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.services.scalatest.dstest.DistageAbstractScalatestSpec
import izumi.functional.bio.{BIOApplicative, F}
import logstage.LogBIO

trait DynamoTestEnv[F[+_, +_]] extends DistageAbstractScalatestSpec[F[Throwable, ?]] {
  implicit def tagBIO: TagKK[F]

  override protected def config: TestConfig = super.config.copy(
    moduleOverrides = new ModuleDef {
      make[DDLDown[F]]
    },
    memoizationRoots = Map(Scene.Managed -> DIKey[DDLDown[F]]),
    forcedRoots      = Map(Scene.Managed -> DIKey[DDLDown[F]]),
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
