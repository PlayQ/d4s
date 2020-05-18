package d4s.env

import d4s.config.{DynamoMeta, ProvisionedThroughputConfig, ProvisioningConfig, TableProvisionedThroughputConfig}
import d4s.env.Models._
import d4s.models.table.TableDef
import d4s.test.envs.DynamoTestEnv
import distage.Tag
import izumi.distage.config.ConfigModuleDef
import izumi.distage.constructors.{AnyConstructor, HasConstructor}
import izumi.distage.model.providers.ProviderMagnet
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.{AssertIO, DistageBIOSpecScalatest}
import net.playq.aws.tagging.AwsNameSpace
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import zio.{IO, ZIO}

abstract class DynamoTestBase[Ctx: Tag](implicit val ctor: AnyConstructor[Ctx]) extends DistageBIOSpecScalatest[IO] with DynamoTestEnv[IO] with AssertIO {
  protected[d4s] final def scopeIO(f: Ctx => IO[_, _]): ProviderMagnet[IO[_, Unit]] = ctor.provider.map(f(_).unit)

  protected[d4s] final def scopeZIO[R: HasConstructor](f: Ctx => ZIO[R, _, _]): ProviderMagnet[IO[_, Unit]] = ctor.provider.map2(HasConstructor[R])(f(_).unit.provide(_))

  override def config: TestConfig = super.config.copy(
    pluginConfig = PluginConfig.const(D4STestPlugin),
    moduleOverrides = super.config.moduleOverrides overridenBy new ConfigModuleDef {
        make[DynamoMeta].from {
          DynamoMeta(ProvisioningConfig(cfgDefault, List(TableProvisionedThroughputConfig("table1", cfgForTable1, Nil))), _: AwsNameSpace, None)
        }

        make[InterpreterTestTable]
        make[TestTable1]
        make[TestTable2]
        make[TestTable3]
        make[UpdatedTestTable]
        make[UpdatedTestTable1]
        make[UpdatedTestTable2]

        many[TableDef]
          .ref[TestTable1]
          .ref[TestTable2]
          .ref[TestTable3]
          .ref[UpdatedTestTable]
          .ref[InterpreterTestTable]
      },
    configBaseName = "test",
  )

  private def cfgDefault: ProvisionedThroughputConfig = ProvisionedThroughputConfig.minimal
  private def cfgForTable1                            = ProvisionedThroughputConfig(2L, 2L, BillingMode.PROVISIONED)
}
