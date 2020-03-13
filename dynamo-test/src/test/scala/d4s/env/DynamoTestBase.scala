package d4s.env

import d4s.config.{DynamoMeta, ProvisionedThroughputConfig, ProvisioningConfig, TableProvisionedThroughputConfig}
import d4s.env.Models._
import d4s.models.table.TableDef
import d4s.test.envs.DynamoTestEnv
import distage.Tag
import izumi.distage.constructors.AnyConstructor
import izumi.distage.model.definition.{Module, ModuleDef}
import izumi.distage.model.providers.ProviderMagnet
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.{AssertIO, DistageBIOSpecScalatest}
import net.playq.aws.tagging.AwsNameSpace
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import zio.IO

abstract class DynamoTestBase[Ctx: Tag](implicit val ctor: AnyConstructor[Ctx]) extends DistageBIOSpecScalatest[IO] with DynamoTestEnv with AssertIO {
  protected[d4s] final def scopeIO(f: Ctx => IO[_, _]): ProviderMagnet[IO[_, _]] = ctor.provider.map(f)

  override def config: TestConfig = TestConfig(
    pluginConfig     = PluginConfig.const(D4STestPlugin),
    memoizationRoots = memoizationRoots,
    forcedRoots      = additionalRoots,
    moduleOverrides  = moduleOverrides,
    configBaseName   = "test",
    // Set parallel envs to false so that memoized docker from the previous environment is reused when the next environment starts up
    // (FIXME: distage-docker currently does not handle multiples of the same docker starting at the same time)
    parallelEnvs = false
  )

  override def moduleOverrides: Module = super.moduleOverrides overridenBy new ModuleDef {
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
