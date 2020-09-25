package d4s

import d4s.models.table.TableDef
import izumi.distage.model.definition.DIResource
import izumi.functional.bio.{BIOAsync, BIOMonad}
import logstage.LogBIO
import logstage.LogBIO.log

final class DynamoDDLService[F[+_, +_]: BIOMonad: LogBIO](
  instances: Set[TableDef],
  tablesManager: DynamoTablesService[F],
) extends DIResource.SelfNoClose[F[Throwable, ?], DynamoDDLService[F]] {

  override def acquire: F[Throwable, Unit] = {
    for {
      _ <- log.info(s"Dynamo performing up scripts...")
      _ <- tablesManager.create(instances)
      _ <- log.info("Upping finished")
    } yield ()
  }

  def down(): F[Throwable, Unit] = {
    for {
      _ <- log.info(s"performing down scripts...")
      _ <- tablesManager.delete(instances)
      _ <- log.info("downing finished")
    } yield ()
  }
}
