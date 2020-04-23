package d4s.env

import d4s.config.{DynamoMeta, ProvisionedThroughputConfig, ProvisioningConfig, TableProvisionedThroughputConfig}
import d4s.env.Models._
import d4s.models.table.TableDef
import d4s.test.envs.DynamoTestEnv
import distage.Tag
import izumi.distage.config.ConfigModuleDef
import izumi.distage.constructors.AnyConstructor
import izumi.distage.model.definition.Module
import izumi.distage.model.providers.ProviderMagnet
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.AssertIO
import net.playq.aws.tagging.AwsNameSpace
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import zio.IO

abstract class DynamoTestBase[Ctx: Tag](implicit val ctor: AnyConstructor[Ctx]) extends DynamoTestEnv with AssertIO {
  protected[d4s] final def scopeIO(f: Ctx => IO[_, _]): ProviderMagnet[IO[_, Unit]] = ctor.provider.map(f(_).unit)

  override def config: TestConfig = super.config.copy(
    pluginConfig    = PluginConfig.const(D4STestPlugin),
    moduleOverrides = moduleOverrides,
    configBaseName  = "test",
  )

  override def moduleOverrides: Module = super.moduleOverrides overridenBy new ConfigModuleDef {
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
  }

  private def cfgDefault: ProvisionedThroughputConfig = ProvisionedThroughputConfig.minimal
  private def cfgForTable1                            = ProvisionedThroughputConfig(2L, 2L, BillingMode.PROVISIONED)
}
