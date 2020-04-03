package d4s.modules

import d4s._
import d4s.config._
import d4s.health.DynamoDBHealthChecker
import d4s.models.table.TableDef
import distage.config.ConfigModuleDef
import distage.{ModuleDef, TagKK}
import izumi.fundamentals.platform.integration.PortCheck
import net.playq.aws.tagging.AwsNameSpace

class D4SModule[F[+_, +_]: TagKK] extends ModuleDef {
  include(D4SModule.base[F])
  include(D4SModule.configs)
}

object D4SModule {
  def apply[F[+_, +_]: TagKK]: ModuleDef = new D4SModule[F]

  def base[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
    make[DynamoClient[F]].from[DynamoClient.Impl[F]]
    make[DynamoConnector[F]].from[DynamoConnector.Impl[F]]
    make[DynamoInterpreter[F]].from[DynamoInterpreter.Impl[F]]
    make[DynamoTablesService[F]].from[DynamoTablesService.Impl[F]]

    make[DynamoDBHealthChecker[F]]
    make[DynamoDDLService[F]].fromResource[DynamoDDLService[F]]
    make[DynamoComponent].fromResource[DynamoComponent.Impl[F]]

    many[TableDef]

    make[PortCheck].named("dynamo-port").from(new PortCheck(3))
  }

  val configs: ConfigModuleDef = new ConfigModuleDef {
    make[DynamoMeta].from {
      (provisioningConf: ProvisioningConfig, namespace: AwsNameSpace, dynamoConf: DynamoConfig) =>
        dynamoConf.maybeLocalUrl match {
          case Some(_) => DynamoMeta(ProvisioningConfig(ProvisionedThroughputConfig.minimal, Nil), namespace, None)
          case None    => DynamoMeta(provisioningConf, namespace, dynamoConf.backupEnabled)
        }
    }

    makeConfig[DynamoConfig]("aws.dynamo")
    makeConfig[DynamoBatchConfig]("aws.dynamo")
    makeConfig[ProvisioningConfig]("aws.dynamo.provisioning")
  }
}
