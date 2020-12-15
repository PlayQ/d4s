package d4s.test.envs

import d4s.DynamoDDLService
import d4s.test.envs.DynamoTestEnv.DDLDown
import distage.{DIKey, ModuleDef, TagKK}
import izumi.distage.model.definition.DIResource
import izumi.distage.model.definition.StandardAxis.Scene
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.services.scalatest.dstest.DistageAbstractScalatestSpec
import izumi.functional.bio.{Applicative2, F}
import logstage.LogIO2

trait DynamoTestEnv[F[+_, +_]] extends DistageAbstractScalatestSpec[F[Throwable, ?]] {
  implicit def tagBIO: TagKK[F]

  override protected def config: TestConfig = super.config.copy(
    moduleOverrides = super.config.moduleOverrides ++ new ModuleDef {
      make[DDLDown[F]]
    },
    memoizationRoots = super.config.memoizationRoots ++ Map(
      Set(Scene.Managed) -> Set(DIKey[DDLDown[F]])
    ),
    forcedRoots = super.config.forcedRoots ++ Map(
      Set(Scene.Managed) -> Set(DIKey[DDLDown[F]])
    ),
  )
}

object DynamoTestEnv {
  final case class DDLDown[F[+_, +_]: Applicative2](
    dynamoDDLService: DynamoDDLService[F],
    logger: LogIO2[F],
  ) extends DIResource.Self[F[Throwable, ?], DDLDown[F]] {
    override def acquire: F[Throwable, Unit] = F.unit
    override def release: F[Throwable, Unit] = {
      logger.info("Deleting all tables") *>
      dynamoDDLService.down()
    }
  }
}
